package com.peto.ramap.ui.onboarding.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.onboarding.guide.log.GuideAnalytics
import com.peto.ramap.ui.onboarding.shopselection.ShopSelectionViewModel
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionSideEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
public fun OnboardingGuideRoute(
    onCompleted: (Int) -> Unit,
    analytics: GuideAnalytics,
    toastManager: ToastManager = koinInject(),
    viewModel: ShopSelectionViewModel = koinViewModel(parameters = { parametersOf(analytics) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is ShopSelectionSideEffect.Completed -> onCompleted(effect.savedShopCount)
            is ShopSelectionSideEffect.ShowToast -> toastManager.show(effect.data)
        }
    }
    OnboardingGuideContent(
        uiState = state,
        onIntent = viewModel::dispatch,
        onStageViewed = analytics::viewStage,
        onGuideAction = analytics::guideAction,
        onShopGoalReached = analytics::shopGoalReached,
    )
}
