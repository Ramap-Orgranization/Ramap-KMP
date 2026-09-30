package com.peto.ramap.debug.admin.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginIntent
import com.peto.ramap.debug.admin.ui.registration.AdminRegistrationRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AdminLoginRoute(
    onBack: () -> Unit,
    viewModel: AdminLoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.isAuthorized) {
        AdminRegistrationRoute()
    } else {
        AdminLoginScreen(
            uiState = uiState,
            onBack = onBack,
            onAdminLogin = { viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked) },
        )
    }
}
