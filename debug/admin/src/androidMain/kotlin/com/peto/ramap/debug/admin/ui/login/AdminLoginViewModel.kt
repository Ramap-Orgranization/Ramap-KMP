package com.peto.ramap.debug.admin.ui.login

import com.peto.ramap.debug.admin.data.datasource.AdminAccessDataSource
import com.peto.ramap.debug.admin.data.datasource.AdminAuthDataSource
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginError
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginIntent
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginLoadKey
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginSideEffect
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginUiState
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException

internal class AdminLoginViewModel(
    private val authDataSource: AdminAuthDataSource,
    private val accessDataSource: AdminAccessDataSource,
) : BaseViewModel<AdminLoginUiState, AdminLoginIntent, AdminLoginSideEffect>(AdminLoginUiState()) {
    override suspend fun handleIntent(intent: AdminLoginIntent) {
        when (intent) {
            is AdminLoginIntent.OnAdminLoginClicked -> signInAsAdministrator(intent.email, intent.password)
        }
    }

    private fun signInAsAdministrator(
        email: String,
        password: String,
    ) {
        launchTask(
            taskKey = LOGIN_TASK_KEY,
            loadKey = AdminLoginLoadKey.Login,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(isAuthorized = false, error = null) },
        ) {
            try {
                authDataSource.signIn(email.trim(), password)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleError(exception)
                reduce { copy(error = AdminLoginError.LoginFailed) }
                return@launchTask
            }

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
    }

    private companion object {
        const val LOGIN_TASK_KEY = "admin-login"
    }
}
