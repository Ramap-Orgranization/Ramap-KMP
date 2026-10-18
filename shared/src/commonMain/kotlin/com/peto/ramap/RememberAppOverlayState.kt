package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.navigation.NavigationState
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.koinInject

@Composable
internal fun rememberAppOverlayState(
    navigationState: NavigationState,
    toastManager: ToastManager,
    loginRepository: LoginRepository = koinInject(),
    signInUseCase: SignInUseCase = koinInject(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): OverlayState {
    val sessionState by loginRepository.session.collectAsStateWithLifecycle(
        initialValue =
            if (loginRepository.hasSession()) {
                LoginSessionState.AUTHENTICATED
            } else {
                LoginSessionState.NOT_AUTHENTICATED
            },
    )
    val overlayState =
        remember(loginRepository, navigationState, signInUseCase, toastManager, coroutineScope) {
            OverlayState(
                loginRepository = loginRepository,
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
