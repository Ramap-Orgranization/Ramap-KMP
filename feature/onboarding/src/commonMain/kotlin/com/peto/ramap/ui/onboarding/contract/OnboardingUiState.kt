package com.peto.ramap.ui.onboarding.contract

import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

internal data class OnboardingUiState(
    val hasConfirmedPermissionGuide: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<OnboardingUiState> {
    override fun withLoadingState(loadState: LoadState): OnboardingUiState = copy(loadState = loadState)
}
