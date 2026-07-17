package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLoginRepository(
    initialSessionState: LoginSessionState = LoginSessionState.NOT_AUTHENTICATED,
    private val userEmail: String? = null,
) : LoginRepository {
    private val mutableSessionState = MutableStateFlow(initialSessionState)

    var signInWithKakaoCallCount = 0
        private set
    var signOutCallCount = 0
        private set
    var deleteAccountCallCount = 0
        private set
    var deleteAccountError: Throwable? = null

    override val sessionState: Flow<LoginSessionState> = mutableSessionState

    override suspend fun awaitInitialization() = Unit

    override fun hasSession(): Boolean = mutableSessionState.value == LoginSessionState.AUTHENTICATED

    override fun currentUserEmail(): String? = userEmail

    override suspend fun signInWithKakao(): RamapResult<Unit> {
        signInWithKakaoCallCount += 1
        return RamapResult.Success(Unit)
    }

    override suspend fun signOut(): RamapResult<Unit> {
        signOutCallCount += 1
        mutableSessionState.value = LoginSessionState.NOT_AUTHENTICATED
        return RamapResult.Success(Unit)
    }

    override suspend fun deleteAccount(): RamapResult<Unit> {
        deleteAccountCallCount += 1
        deleteAccountError?.let { return RamapResult.Error(RamapError.Unknown(it)) }
        mutableSessionState.value = LoginSessionState.NOT_AUTHENTICATED
        return RamapResult.Success(Unit)
    }
}
