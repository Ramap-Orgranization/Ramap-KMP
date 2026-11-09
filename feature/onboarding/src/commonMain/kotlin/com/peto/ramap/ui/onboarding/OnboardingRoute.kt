package com.peto.ramap.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.platform.permission.rememberNotificationPermissionGenerator
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingPermission
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.log.OnboardingAnalyticsStage
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    onCompleted: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val lifecycleOwner = LocalLifecycleOwner.current
    val analytics = viewModel.analytics
    val locationPermission =
        rememberLocationPermissionGenerator {
            viewModel.dispatch(OnboardingIntent.LocationPermissionFinished(it))
        }
    val notificationPermission =
        rememberNotificationPermissionGenerator {
            viewModel.dispatch(OnboardingIntent.NotificationPermissionFinished(it))
        }

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

    LaunchedEffect(state.step, analytics) {
        if (state.step == OnboardingStep.PermissionGuide) analytics.viewStage(OnboardingAnalyticsStage.PermissionGuide)
    }

    LaunchedEffect(viewModel, lifecycleOwner, locationPermission, notificationPermission) {
        // 앞선 시스템 팝업이 닫히고 앱이 활성화된 뒤 다음 권한을 요청한다.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    OnboardingSideEffect.Completed -> currentOnCompleted()
                    is OnboardingSideEffect.RequestPermission ->
                        when (effect.permission) {
                            OnboardingPermission.Location -> locationPermission.requestPermission()
                            OnboardingPermission.Notification -> notificationPermission.requestPermission()
                        }
                }
            }
        }
    }

    OnboardingContent(
        uiState = state,
        onIntent = viewModel::dispatch,
    ) {
        OnboardingGuideRoute(
            onCompleted = { viewModel.dispatch(OnboardingIntent.ShopSelectionCompleted) },
            analytics = analytics,
            toastManager = toastManager,
        )
    }
}
