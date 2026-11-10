package com.peto.ramap.data.repository

import app.cash.turbine.test
import com.peto.ramap.data.auth.SignupConsentSignIn
import com.peto.ramap.fixture.authenticatedSessionFixture
import com.peto.ramap.fixture.withSupabaseSessionFixture
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(SupabaseInternal::class)
class LoginAccountStateTest {
    @Test
    fun `기기 설정에 다녀와 같은 세션을 복원해도 계정 변경을 알리지 않는다`() =
        runTest {
            withSupabaseSessionFixture { client ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                DefaultLoginRepository(client, SignupConsentSignIn(client) { error("가입 동의를 요청하면 안 됩니다.") }).accountId.test {
                    assertEquals("viewer", awaitItem())
                    client.auth.setSessionStatus(SessionStatus.Initializing)
                    expectNoEvents()
                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    expectNoEvents()
                    client.auth.setSessionStatus(authenticatedSessionFixture("other-viewer"))
                    assertEquals("other-viewer", awaitItem())
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated(isSignOut = true))
                    assertNull(awaitItem())
                }
            }
        }

    @Test
    fun `일시적인 토큰 갱신 실패는 계정 해제로 알리지 않는다`() =
        runTest {
            withSupabaseSessionFixture { client ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                DefaultLoginRepository(client, SignupConsentSignIn(client) { error("가입 동의를 요청하면 안 됩니다.") }).accountId.test {
                    assertEquals("viewer", awaitItem())
                    client.auth.setSessionStatus(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException("offline"))))
                    expectNoEvents()
                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    expectNoEvents()
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
                    assertNull(awaitItem())
                }
            }
        }

    @Test
    fun `초기 세션 확인이 끝나기 전에는 게스트 계정을 알리지 않는다`() =
        runTest {
            withSupabaseSessionFixture { client ->
                DefaultLoginRepository(client, SignupConsentSignIn(client) { error("가입 동의를 요청하면 안 됩니다.") }).accountId.test {
                    expectNoEvents()
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
                    assertNull(awaitItem())
                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    assertEquals("viewer", awaitItem())
                }
            }
        }
}
