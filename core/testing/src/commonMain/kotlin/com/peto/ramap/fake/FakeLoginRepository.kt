package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLoginRepository(
    initialSessionState: LoginSessionState = LoginSessionState.NOT_AUTHENTICATED,
    private val userEmail: String? = null,
    initialAccountId: String = "test-account",
) : LoginRepository {
    private val mutableAccountId = MutableStateFlow(initialAccountId.takeIf { initialSessionState == LoginSessionState.AUTHENTICATED })
    override val accountId: Flow<String?> = mutableAccountId

    override fun currentAccountId(): String? = mutableAccountId.value

    fun updateAccountId(accountId: String?) {
        mutableAccountId.value = accountId
        mutableSessionState.value = if (accountId == null) LoginSessionState.NOT_AUTHENTICATED else LoginSessionState.AUTHENTICATED
    }

    private val mutableSessionState = MutableStateFlow(initialSessionState)

    var signInWithKakaoCallCount = 0
        private set
    var signInWithAppleCallCount = 0
        private set
    var signOutCallCount = 0
        private set
    var deleteAccountCallCount = 0
        private set
    var deleteAccountError: Throwable? = null
    var signInResult: RamapResult<Unit> = RamapResult.Success(Unit)

    override val session: Flow<LoginSessionState> = mutableSessionState

    override suspend fun awaitInitialization() = Unit

    fun updateSessionState(sessionState: LoginSessionState) {
        mutableAccountId.value = if (sessionState == LoginSessionState.AUTHENTICATED) mutableAccountId.value ?: "test-account" else null
        mutableSessionState.value = sessionState
    }

    override fun hasSession(): Boolean = mutableSessionState.value == LoginSessionState.AUTHENTICATED

    override fun currentUserEmail(): String? = userEmail

    override suspend fun signIn(type: LoginType): RamapResult<Unit> {
        when (type) {
            LoginType.KAKAO -> signInWithKakaoCallCount += 1
            LoginType.APPLE -> signInWithAppleCallCount += 1
        }
        return signInResult
    }

    override suspend fun signOut(): RamapResult<Unit> {
        signOutCallCount += 1
        updateAccountId(null)
        return RamapResult.Success(Unit)
    }

    override suspend fun deleteAccount(): RamapResult<Unit> {
        deleteAccountCallCount += 1
        deleteAccountError?.let { return RamapResult.Error(RamapError.Unknown(it)) }
        updateAccountId(null)
        return RamapResult.Success(Unit)
    }
}
