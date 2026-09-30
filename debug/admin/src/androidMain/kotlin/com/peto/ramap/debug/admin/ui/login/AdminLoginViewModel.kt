package com.peto.ramap.debug.admin.ui.login

import androidx.lifecycle.viewModelScope
import com.peto.ramap.debug.admin.data.datasource.AdminAccessDataSource
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginError
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginIntent
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginLoadKey
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginSideEffect
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginUiState
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class AdminLoginViewModel(
    private val loginRepository: LoginRepository,
    private val accessDataSource: AdminAccessDataSource,
) : BaseViewModel<AdminLoginUiState, AdminLoginIntent, AdminLoginSideEffect>(AdminLoginUiState()) {
    init {
        observeSessionState()
    }

    override suspend fun handleIntent(intent: AdminLoginIntent) {
        when (intent) {
            AdminLoginIntent.OnAdminLoginClicked -> signInAsAdministrator()
        }
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            loginRepository.sessionState.collectLatest { sessionState ->
                if (sessionState == LoginSessionState.NOT_AUTHENTICATED) {
                    reduce { copy(isAuthorized = false) }
                }
            }
        }
    }

    private fun signInAsAdministrator() {
        launchResultTask(
            taskKey = LOGIN_TASK_KEY,
            loadKey = AdminLoginLoadKey.Login,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(error = null) },
            request = { loginRepository.signIn(LoginType.KAKAO) },
            onSuccess = { verifyAccess() },
            onError = { reduce { copy(error = AdminLoginError.LoginFailed) } },
        )
    }

    private suspend fun verifyAccess() {
        try {
            val isAuthorized = accessDataSource.hasAccess()
            reduce {
                copy(
                    isAuthorized = isAuthorized,
                    error = if (isAuthorized) null else AdminLoginError.AccessDenied,
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            handleError(exception)
            reduce { copy(error = AdminLoginError.AccessUnavailable) }
        }
    }

    private companion object {
        const val LOGIN_TASK_KEY = "admin-login"
    }
}
