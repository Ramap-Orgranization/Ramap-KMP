package com.peto.ramap.data.repository

import app.cash.turbine.test
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fixture.authenticatedSessionFixture
import com.peto.ramap.fixture.withSupabaseSessionFixture
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(SupabaseInternal::class)
class LoginSessionStateTest {
    @Test
    fun `토큰 갱신이 일시 실패해도 로그아웃을 알리지 않는다`() =
        runTest {
            withSupabaseSessionFixture { client ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                DefaultLoginRepository(client).session.test {
                    assertEquals(LoginSessionState.AUTHENTICATED, awaitItem())
                    client.auth.setSessionStatus(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException("offline"))))
                    expectNoEvents()
                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    assertEquals(LoginSessionState.AUTHENTICATED, awaitItem())
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated(isSignOut = true))
                    assertEquals(LoginSessionState.NOT_AUTHENTICATED, awaitItem())
                }
            }
        }

    @Test
    fun `갱신 실패 중 새 구독에도 로그아웃을 알리지 않는다`() =
        runTest {
            withSupabaseSessionFixture { client ->
                client.auth.setSessionStatus(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException("offline"))))
                DefaultLoginRepository(client).session.test {
                    expectNoEvents()
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
                    assertEquals(LoginSessionState.NOT_AUTHENTICATED, awaitItem())
                }
            }
        }

    @Test
    fun `백그라운드 세션 복원을 로그아웃으로 알리지 않는다`() =
        runTest {
            withSupabaseSessionFixture { client ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                DefaultLoginRepository(client).session.test {
                    assertEquals(LoginSessionState.AUTHENTICATED, awaitItem())

                    client.auth.setSessionStatus(SessionStatus.Initializing)
                    expectNoEvents()
                    client.auth.setSessionStatus(authenticatedSessionFixture())
                    assertEquals(LoginSessionState.AUTHENTICATED, awaitItem())

                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
                    assertEquals(LoginSessionState.NOT_AUTHENTICATED, awaitItem())
                }
            }
        }

    @Test
    fun `초기 세션 확인이 끝나야 로그인 여부를 알린다`() =
        runTest {
            withSupabaseSessionFixture { client ->
                DefaultLoginRepository(client).session.test {
                    expectNoEvents()
                    client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
                    assertEquals(LoginSessionState.NOT_AUTHENTICATED, awaitItem())
                }
            }
        }

    @Test
    fun `Supabase 세션 상태를 도메인 로그인 상태로 변환한다`() {
        val authenticated =
            SessionStatus.Authenticated(
                UserSession(
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    expiresIn = 3600,
                    tokenType = "bearer",
                ),
            )

        assertEquals(LoginSessionState.AUTHENTICATED, loginSessionState(authenticated))
        assertEquals(LoginSessionState.NOT_AUTHENTICATED, loginSessionState(SessionStatus.NotAuthenticated()))
        assertNull(loginSessionState(SessionStatus.Initializing))
        assertNull(loginSessionState(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException("offline")))))
    }
}
