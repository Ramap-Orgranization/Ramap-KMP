package com.peto.ramap.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.guide.OnboardingGuideRoute
import com.peto.ramap.ui.onboarding.guide.log.OnboardingAnalyticsStage
import com.peto.ramap.ui.onboarding.permission.PermissionGuideRoute
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    onCompleted: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val lifecycleOwner = LocalLifecycleOwner.current
    val analytics = viewModel.analytics

    DisposableEffect(lifecycleOwner, analytics) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> analytics.backgrounded()
                    Lifecycle.Event.ON_START -> analytics.foregrounded()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.step, analytics) {
        if (uiState.step == OnboardingStep.PermissionGuide) analytics.viewStage(OnboardingAnalyticsStage.PermissionGuide)
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.sideEffect.collect { effect ->
                if (effect is OnboardingSideEffect.Completed) {
                    currentOnCompleted()
                }
            }
        }
    }

    val requestPermissionFlow =
        remember(viewModel.sideEffect) {
            viewModel.sideEffect
                .filterIsInstance<OnboardingSideEffect.RequestPermission>()
                .map { it.permission }
        }

    OnboardingContent(
        uiState = uiState,
        onIntent = viewModel::dispatch,
        permissionContent = {
            PermissionGuideRoute(
                isConfirming = uiState.loadState.isLoading(OnboardingLoadKey.ConfirmPermissionGuide),
                onRequestPermission = requestPermissionFlow,
                onConfirm = { viewModel.dispatch(OnboardingIntent.ConfirmPermissionGuide) },
                onLocationPermissionFinished = { viewModel.dispatch(OnboardingIntent.LocationPermissionFinished(it)) },
                onNotificationPermissionFinished = { viewModel.dispatch(OnboardingIntent.NotificationPermissionFinished(it)) },
            )
        },
        guideContent = {
            OnboardingGuideRoute(
                onCompleted = { viewModel.dispatch(OnboardingIntent.ShopSelectionCompleted) },
                analytics = analytics,
                toastManager = toastManager,
            )
        },
    )
}
