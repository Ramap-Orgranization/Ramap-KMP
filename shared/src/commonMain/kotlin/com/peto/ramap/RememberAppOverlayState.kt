package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.usecase.LoginSessionUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.navigation.NavigationState
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.koinInject

@Composable
internal fun rememberAppOverlayState(
    navigationState: NavigationState,
    toastManager: ToastManager,
    loginSessionUseCase: LoginSessionUseCase = koinInject(),
    signInUseCase: SignInUseCase = koinInject(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): OverlayState {
    val sessionState by loginSessionUseCase().collectAsStateWithLifecycle(
        initialValue =
            if (loginSessionUseCase.hasSession()) {
                LoginSessionState.AUTHENTICATED
            } else {
                LoginSessionState.NOT_AUTHENTICATED
            },
    )
    val overlayState =
        remember(loginSessionUseCase, navigationState, signInUseCase, toastManager, coroutineScope) {
            OverlayState(
                loginSessionUseCase = loginSessionUseCase,
                navigationState = navigationState,
                signInUseCase = signInUseCase,
                toastManager = toastManager,
                coroutineScope = coroutineScope,
            )
        }

    LaunchedEffect(sessionState, overlayState.pendingProfileTarget) {
        if (sessionState != LoginSessionState.AUTHENTICATED) return@LaunchedEffect
        val (userId, asOverlay) = overlayState.pendingProfileTarget ?: return@LaunchedEffect
        overlayState.pendingProfileTarget = null
        overlayState.showProfileLoginGuide = false
        overlayState.openProfile(userId, asOverlay)
    }

    return overlayState
}
