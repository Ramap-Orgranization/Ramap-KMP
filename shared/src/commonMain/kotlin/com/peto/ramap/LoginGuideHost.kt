package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.ui.login.LoginGuideController

@Composable
internal fun LoginGuideHost(controller: LoginGuideController) {
    val currentRequest by controller.request.collectAsStateWithLifecycle()
    val request = currentRequest ?: return

    key(request) {
        LoginGuideDialog(
            visible = true,
            onDismiss = { controller.dismiss(request) },
            onLoginTypeSelected = { controller.select(request, it) },
            loginButton = { type, onClick -> LoginButton(type, onClick) },
        )
    }
}
