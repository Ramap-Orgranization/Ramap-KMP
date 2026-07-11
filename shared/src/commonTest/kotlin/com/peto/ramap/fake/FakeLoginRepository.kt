package com.peto.ramap.fake

import com.peto.ramap.domain.repository.LoginRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeLoginRepository(
    initialSessionStatus: SessionStatus = SessionStatus.NotAuthenticated(),
    private val userEmail: String? = null,
) : LoginRepository {
    private val mutableSessionStatus = MutableStateFlow(initialSessionStatus)

    var signInWithKakaoCallCount = 0
        private set
    var signOutCallCount = 0
        private set
    var deleteAccountCallCount = 0
        private set
    var deleteAccountError: Throwable? = null

    override val sessionStatus: StateFlow<SessionStatus> = mutableSessionStatus

    override suspend fun awaitInitialization() = Unit

    override fun hasSession(): Boolean = sessionStatus.value is SessionStatus.Authenticated

    override fun currentUserEmail(): String? = userEmail

    override suspend fun signInWithKakao() {
        signInWithKakaoCallCount += 1
    }

    override suspend fun signOut() {
        signOutCallCount += 1
        mutableSessionStatus.value = SessionStatus.NotAuthenticated(isSignOut = true)
    }

    override suspend fun deleteAccount() {
        deleteAccountCallCount += 1
        deleteAccountError?.let { throw it }
        mutableSessionStatus.value = SessionStatus.NotAuthenticated(isSignOut = true)
    }
}
