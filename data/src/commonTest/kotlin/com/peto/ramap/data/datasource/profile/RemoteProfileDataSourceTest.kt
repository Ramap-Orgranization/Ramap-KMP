package com.peto.ramap.data.datasource.profile

import app.cash.turbine.test
import com.peto.ramap.fixture.authenticatedSessionFixture
import com.peto.ramap.fixture.withSupabaseSessionFixture
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(SupabaseInternal::class)
class RemoteProfileDataSourceTest {
    @Test
    fun `백그라운드 복귀로 같은 세션을 복원해도 사용자 변경을 알리지 않는다`() =
        runTest {
            withSupabaseSessionFixture { client ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                RemoteProfileDataSource(client).sessionUserIds.test {
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
            withSupabaseSessionFixture { client ->
                RemoteProfileDataSource(client).sessionUserIds.test {
                    expectNoEvents()
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
                    assertNull(awaitItem())

                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    assertEquals("viewer", awaitItem())
                }
            }
        }
}
