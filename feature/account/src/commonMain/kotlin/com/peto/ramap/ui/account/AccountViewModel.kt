package com.peto.ramap.ui.account

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.ui.account.contract.AccountIntent
import com.peto.ramap.ui.account.contract.AccountIntent.OnAccountDeleteConfirm
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
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.logout_failure_message

class AccountViewModel(
    private val loginRepository: LoginRepository,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModel<AccountUiState, AccountIntent, AccountSideEffect>(AccountUiState()) {
    init {
        observeSessionState()
    }

    override suspend fun handleIntent(intent: AccountIntent) {
        when (intent) {
            OnKakaoLoginClick -> signInWithKakao()
            OnLogoutClick -> signOut()
            OnAccountDeleteConfirm -> deleteAccount()
        }
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            loginRepository.sessionState.collectLatest { sessionState ->
                val isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED
                reduce {
                    copy(
                        isLoggedIn = isAuthenticated,
                        accountLabel = if (isAuthenticated) loginRepository.currentUserEmail() else null,
                    )
                }
            }
        }
    }

    private fun signInWithKakao() {
        analyticsTracker.logEvent(
            AnalyticsEvents.LOGIN_START,
            mapOf(AnalyticsParams.SOURCE to AnalyticsSource.ACCOUNT),
        )
        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            loadKey = AccountLoadKey.Login,
            policy = TaskPolicy.IgnoreNew,
            request = loginRepository::signInWithKakao,
            onSuccess = {
                analyticsTracker.logEvent(
                    AnalyticsEvents.LOGIN_SUCCESS,
                    mapOf(
                        AnalyticsParams.METHOD to "kakao",
                        AnalyticsParams.SOURCE to AnalyticsSource.ACCOUNT,
                    ),
                )
            },
            onError = {
                analyticsTracker.logEvent(
                    AnalyticsEvents.LOGIN_FAILURE,
                    mapOf(
                        AnalyticsParams.METHOD to "kakao",
                        AnalyticsParams.SOURCE to AnalyticsSource.ACCOUNT,
                    ),
                )
                showToast(Res.string.kakao_login_failure_message, ToastType.ERROR)
            },
        )
    }

    private fun signOut() {
        analyticsTracker.logEvent(AnalyticsEvents.LOGOUT)
        launchResultTask(
            taskKey = SIGN_OUT_TASK_KEY,
            loadKey = AccountLoadKey.Logout,
            policy = TaskPolicy.IgnoreNew,
            request = loginRepository::signOut,
            onError = { showToast(Res.string.logout_failure_message, ToastType.ERROR) },
        )
    }

    private fun deleteAccount() {
        analyticsTracker.logEvent(AnalyticsEvents.ACCOUNT_DELETE)
        launchResultTask(
            taskKey = DELETE_ACCOUNT_TASK_KEY,
            loadKey = AccountLoadKey.Delete,
            policy = TaskPolicy.IgnoreNew,
            request = loginRepository::deleteAccount,
            onSuccess = { showToast(Res.string.account_delete_success_message, ToastType.SUCCESS) },
            onError = {
                showToast(Res.string.account_delete_failure_message, ToastType.ERROR)
            },
        )
    }

    private fun showToast(
        messageResource: StringResource,
        type: ToastType,
    ) {
        viewModelScope.launch {
            postSideEffect(ShowToast(ToastData(messageResource, type)))
        }
    }

    companion object {
        private const val SIGN_IN_TASK_KEY = "account-sign-in"
        private const val SIGN_OUT_TASK_KEY = "account-sign-out"
        private const val DELETE_ACCOUNT_TASK_KEY = "account-delete"
    }
}
