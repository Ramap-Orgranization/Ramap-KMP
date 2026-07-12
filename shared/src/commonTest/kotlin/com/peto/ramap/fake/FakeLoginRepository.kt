package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
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

    override suspend fun signInWithKakao(): RamapResult<Unit> {
        signInWithKakaoCallCount += 1
        return RamapResult.Success(Unit)
    }

    override suspend fun signOut(): RamapResult<Unit> {
        signOutCallCount += 1
        mutableSessionStatus.value = SessionStatus.NotAuthenticated(isSignOut = true)
        return RamapResult.Success(Unit)
    }

    override suspend fun deleteAccount(): RamapResult<Unit> {
        deleteAccountCallCount += 1
        deleteAccountError?.let { return RamapResult.Error(RamapError.Unknown(it)) }
        mutableSessionStatus.value = SessionStatus.NotAuthenticated(isSignOut = true)
        return RamapResult.Success(Unit)
    }
}
