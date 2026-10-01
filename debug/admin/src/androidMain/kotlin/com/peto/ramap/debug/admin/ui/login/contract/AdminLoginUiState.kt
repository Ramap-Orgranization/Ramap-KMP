package com.peto.ramap.debug.admin.ui.login.contract

import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

internal data class AdminLoginUiState(
    val isAuthorized: Boolean = false,
    val error: AdminLoginError? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<AdminLoginUiState> {
    override fun withLoadingState(loadState: LoadState): AdminLoginUiState = copy(loadState = loadState)
}

internal enum class AdminLoginError {
    AccessDenied,
    AccessUnavailable,
    LoginFailed,
}
