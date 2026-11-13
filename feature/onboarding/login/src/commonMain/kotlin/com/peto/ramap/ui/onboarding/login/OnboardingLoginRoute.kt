package com.peto.ramap.ui.onboarding.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.onboarding.login.contract.OnboardingLoginIntent
import com.peto.ramap.ui.onboarding.login.contract.OnboardingLoginSideEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingLoginRoute(
    isCompleting: Boolean,
    onCompleted: () -> Unit,
    toastManager: ToastManager,
) {
    val viewModel: OnboardingLoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val currentToastManager by rememberUpdatedState(toastManager)

    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            OnboardingLoginSideEffect.Completed -> currentOnCompleted()
            is OnboardingLoginSideEffect.ShowToast -> currentToastManager.show(effect.data)
        }
    }
    OnboardingLoginContent(
        isSigningIn = state.isSigningIn,
        isCompleting = isCompleting,
        onLoginTypeSelected = { viewModel.dispatch(OnboardingLoginIntent.SelectLoginType(it)) },
        onContinueWithoutLogin = { viewModel.dispatch(OnboardingLoginIntent.ContinueWithoutLogin) },
    )
}
