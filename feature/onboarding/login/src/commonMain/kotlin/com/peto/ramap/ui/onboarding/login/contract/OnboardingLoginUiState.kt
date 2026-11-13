package com.peto.ramap.ui.onboarding.login.contract

import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

internal data class OnboardingLoginUiState(
    override val loadState: LoadState = LoadState(),
) : LoadableState<OnboardingLoginUiState> {
    val isSigningIn: Boolean get() = loadState.isLoading(OnboardingLoginLoadKey.SignIn)

    override fun withLoadingState(loadState: LoadState): OnboardingLoginUiState = copy(loadState = loadState)
}
