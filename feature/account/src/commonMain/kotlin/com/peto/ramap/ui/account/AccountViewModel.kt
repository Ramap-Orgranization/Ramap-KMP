package com.peto.ramap.ui.account

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.ui.account.contract.AccountIntent
import com.peto.ramap.ui.account.contract.AccountIntent.OnAccountDeleteConfirm
import com.peto.ramap.ui.account.contract.AccountIntent.OnKakaoLoginClick
import com.peto.ramap.ui.account.contract.AccountIntent.OnLogoutClick
import com.peto.ramap.ui.account.contract.AccountSideEffect
import com.peto.ramap.ui.account.contract.AccountSideEffect.ShowToast
import com.peto.ramap.ui.account.contract.AccountUiState
import com.peto.ramap.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_failure_message
import ramap.shared.generated.resources.account_delete_success_message
import ramap.shared.generated.resources.kakao_login_failure_message

class AccountViewModel(
    private val loginRepository: LoginRepository,
) : BaseViewModel<AccountUiState, AccountIntent, AccountSideEffect>(AccountUiState()) {
    init {
        viewModelScope.launch { observeSessionState() }
    }

    override suspend fun handleIntent(intent: AccountIntent) {
        when (intent) {
            OnKakaoLoginClick -> signInWithKakao()
            OnLogoutClick -> signOut()
            OnAccountDeleteConfirm -> deleteAccount()
        }
    }

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collectLatest { sessionState ->
            val isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED
            reduce {
                copy(
                    isLoggedIn = isAuthenticated,
                    accountLabel = if (isAuthenticated) loginRepository.currentUserEmail() else null,
                    isDeletingAccount = if (isAuthenticated) isDeletingAccount else false,
                )
            }
        }
    }

    private suspend fun signInWithKakao() {
        handleResult(
            result = loginRepository.signInWithKakao(),
            onError = { showToast(Res.string.kakao_login_failure_message, ToastType.ERROR) },
        )
    }

    private suspend fun signOut() {
        handleResult(result = loginRepository.signOut())
    }

    private suspend fun deleteAccount() {
        if (currentState.isDeletingAccount) return

        reduce { copy(isDeletingAccount = true) }
        handleResult(
            result = loginRepository.deleteAccount(),
            onSuccess = { showToast(Res.string.account_delete_success_message, ToastType.SUCCESS) },
            onError = {
                reduce { copy(isDeletingAccount = false) }
                showToast(Res.string.account_delete_failure_message, ToastType.ERROR)
            },
        )
    }

    private fun showToast(
        messageResource: StringResource,
        type: ToastType,
    ) {
        trySideEffect(ShowToast(ToastData(messageResource, type)))
    }
}
