package com.peto.ramap.data.datasource.profile

import app.cash.turbine.test
import com.peto.ramap.data.datasource.image.RemoteImageUrlDataSource
import com.peto.ramap.fixture.authenticatedSessionFixture
import com.peto.ramap.fixture.withSupabaseSessionFixture
import com.peto.ramap.network.client.media.MediaStorageClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(SupabaseInternal::class)
class RemoteProfileDataSourceTest {
    @Test
    fun `토큰 갱신 실패 중에도 기존 사용자를 유지하고 실제 로그아웃 때 비운다`() =
        runTest {
            withProfileDataSource { client, dataSource ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                dataSource.sessionUserIds.test {
                    assertEquals("viewer", awaitItem())
                    client.auth.setSessionStatus(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException("offline"))))
                    expectNoEvents()
                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    expectNoEvents()
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated(isSignOut = true))
                    assertNull(awaitItem())
                }
            }
        }

    @Test
    fun `백그라운드 복귀로 같은 세션을 복원해도 사용자 변경을 알리지 않는다`() =
        runTest {
            withProfileDataSource { client, dataSource ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                dataSource.sessionUserIds.test {
                    assertEquals("viewer", awaitItem())

                    client.auth.setSessionStatus(SessionStatus.Initializing)
                    expectNoEvents()
                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    expectNoEvents()

                    client.auth.setSessionStatus(authenticatedSessionFixture("other-viewer"))
                    assertEquals("other-viewer", awaitItem())
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
                    assertNull(awaitItem())
                }
            }
        }

    @Test
    fun `초기 세션 복원이 끝난 뒤에만 게스트 상태를 알린다`() =
        runTest {
            withProfileDataSource { client, dataSource ->
                dataSource.sessionUserIds.test {
                    expectNoEvents()
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
                    assertNull(awaitItem())

                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    assertEquals("viewer", awaitItem())
                }
            }
        }

    private suspend fun withProfileDataSource(test: suspend (SupabaseClient, RemoteProfileDataSource) -> Unit) {
        withSupabaseSessionFixture { client ->
            val http = HttpClient(MockEngine { error("예상하지 않은 사진 API") })
            try {
                val dataSource =
                    RemoteProfileDataSource(
                        client = client,
                        imageUrlDataSource = RemoteImageUrlDataSource(client),
                        media = MediaStorageClient(supabase = client, http = http, baseUrl = ""),
                    )
                test(client, dataSource)
            } finally {
                http.close()
            }
        }
    }
}
