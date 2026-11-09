package com.peto.ramap.ui.onboarding.contract

import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Initializing,
    override val loadState: LoadState = LoadState(),
) : LoadableState<OnboardingUiState> {
    override fun withLoadingState(loadState: LoadState): OnboardingUiState = copy(loadState = loadState)
}
