package com.peto.ramap.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.onboarding.contract.ShopSelectionSideEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun OnboardingGuideRoute(
    onCompleted: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: ShopSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            ShopSelectionSideEffect.Completed -> onCompleted()
            is ShopSelectionSideEffect.ShowToast -> toastManager.show(effect.data)
        }
    }
    OnboardingGuideContent(
        uiState = state,
        onIntent = viewModel::dispatch,
    )
}
