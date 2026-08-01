package com.peto.ramap.ui.account

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.analytics.common.login.LoginMethod
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.ui.account.contract.AccountIntent
import com.peto.ramap.ui.account.contract.AccountIntent.OnAccountDeleteConfirm
import com.peto.ramap.ui.account.contract.AccountIntent.OnAppleLoginClick
import com.peto.ramap.ui.account.contract.AccountIntent.OnKakaoLoginClick
import com.peto.ramap.ui.account.contract.AccountIntent.OnLogoutClick
import com.peto.ramap.ui.account.contract.AccountLoadKey
import com.peto.ramap.ui.account.contract.AccountSideEffect
import com.peto.ramap.ui.account.contract.AccountSideEffect.ShowToast
import com.peto.ramap.ui.account.contract.AccountUiState
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_failure_message
import ramap.shared.generated.resources.account_delete_success_message
import ramap.shared.generated.resources.apple_login_failure_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.login_success_message
import ramap.shared.generated.resources.logout_failure_message

class AccountViewModel(
    private val loginRepository: LoginRepository,
    private val loginAnalytics: LoginAnalytics,
) : BaseViewModel<AccountUiState, AccountIntent, AccountSideEffect>(
        AccountUiState(),
    ) {
    init {
        observeSessionState()
    }

    override suspend fun handleIntent(intent: AccountIntent) {
        when (intent) {
            OnKakaoLoginClick -> signInWithKakao()
            OnAppleLoginClick -> signInWithApple()
            OnLogoutClick -> signOut()
            OnAccountDeleteConfirm -> deleteAccount()
        }
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            loginRepository.sessionState.collectLatest { sessionState ->
                updateSessionState(sessionState)
            }
        }
    }

    private fun updateSessionState(sessionState: LoginSessionState) {
        val isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED
        val accountLabel = findAccountLabel(isAuthenticated)

        reduce {
            copy(
                isLoggedIn = isAuthenticated,
                accountLabel = accountLabel,
            )
        }
    }

    private fun findAccountLabel(isAuthenticated: Boolean): String? {
        if (!isAuthenticated) return null

        return loginRepository.currentUserEmail()
    }

    private fun signInWithKakao() {
        loginAnalytics.logLoginStarted(AnalyticsSource.ACCOUNT)

        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            loadKey = AccountLoadKey.Login,
            policy = TaskPolicy.IgnoreNew,
            request = { loginRepository.signIn(LoginType.KAKAO) },
            onSuccess = {
                loginAnalytics.logLoginSucceeded(AnalyticsSource.ACCOUNT)
                showToast(Res.string.login_success_message, ToastType.SUCCESS)
            },
            onError = {
                loginAnalytics.logLoginFailed(AnalyticsSource.ACCOUNT)
                showToast(
                    messageResource = Res.string.kakao_login_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun signInWithApple() {
        loginAnalytics.logLoginStarted(AnalyticsSource.ACCOUNT, LoginMethod.APPLE)

        launchResultTask(
            taskKey = SIGN_IN_WITH_APPLE_TASK_KEY,
            loadKey = AccountLoadKey.Login,
            policy = TaskPolicy.IgnoreNew,
            request = { loginRepository.signIn(LoginType.APPLE) },
            onSuccess = {
                loginAnalytics.logLoginSucceeded(AnalyticsSource.ACCOUNT, LoginMethod.APPLE)
                showToast(Res.string.login_success_message, ToastType.SUCCESS)
            },
            onError = {
                loginAnalytics.logLoginFailed(AnalyticsSource.ACCOUNT, LoginMethod.APPLE)
                showToast(
                    messageResource = Res.string.apple_login_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun signOut() {
        launchResultTask(
            taskKey = SIGN_OUT_TASK_KEY,
            loadKey = AccountLoadKey.Logout,
            policy = TaskPolicy.IgnoreNew,
            request = loginRepository::signOut,
            onError = {
                showToast(
                    messageResource = Res.string.logout_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun deleteAccount() {
        launchResultTask(
            taskKey = DELETE_ACCOUNT_TASK_KEY,
            loadKey = AccountLoadKey.Delete,
            policy = TaskPolicy.IgnoreNew,
            request = loginRepository::deleteAccount,
            onSuccess = {
                showToast(
                    messageResource = Res.string.account_delete_success_message,
                    type = ToastType.SUCCESS,
                )
            },
            onError = {
                showToast(
                    messageResource = Res.string.account_delete_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun showToast(
        messageResource: StringResource,
        type: ToastType,
    ) {
        viewModelScope.launch {
            postSideEffect(
                ShowToast(
                    ToastData(
                        message = messageResource,
                        type = type,
                    ),
                ),
            )
        }
    }

    companion object {
        private const val SIGN_IN_TASK_KEY = "account-sign-in"
        private const val SIGN_IN_WITH_APPLE_TASK_KEY = "account-sign-in-with-apple"
        private const val SIGN_OUT_TASK_KEY = "account-sign-out"
        private const val DELETE_ACCOUNT_TASK_KEY = "account-delete"
    }
}
