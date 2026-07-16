package com.peto.ramap.data.repository

import com.peto.ramap.domain.model.LoginSessionState
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginSessionStateTest {
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
    }
}
