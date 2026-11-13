package com.peto.ramap.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.platform.permission.rememberNotificationPermissionGenerator
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.contract.OnboardingUiState
import com.peto.ramap.ui.onboarding.guide.OnboardingGuideRoute
import com.peto.ramap.ui.onboarding.guide.log.OnboardingAnalyticsStage
import com.peto.ramap.ui.onboarding.log.OnboardingAnalytics
import com.peto.ramap.ui.onboarding.login.OnboardingLoginRoute
import com.peto.ramap.ui.onboarding.permission.PermissionGuideContent
import com.peto.ramap.ui.onboarding.permission.contract.OnboardingPermission
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

    LaunchedEffect(uiState.step, analytics) {
        when (uiState.step) {
            OnboardingStep.PermissionGuide -> analytics.viewStage(OnboardingAnalyticsStage.PermissionGuide)
            is OnboardingStep.Login -> analytics.viewStage(OnboardingAnalyticsStage.Login)
            else -> Unit
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner, locationPermission, notificationPermission, toastManager) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    OnboardingSideEffect.Completed -> currentOnCompleted()
                    is OnboardingSideEffect.ShowToast -> toastManager.show(effect.data)
                    is OnboardingSideEffect.RequestPermission ->
                        when (effect.permission) {
                            OnboardingPermission.Location -> locationPermission.requestPermission()
                            OnboardingPermission.Notification -> notificationPermission.requestPermission()
                        }
                }
            }
        }
    }

    OnboardingRouteContent(
        uiState = uiState,
        analytics = analytics,
        toastManager = toastManager,
        onIntent = viewModel::dispatch,
    )
}

@Composable
internal fun OnboardingRouteContent(
    uiState: OnboardingUiState,
    analytics: OnboardingAnalytics,
    toastManager: ToastManager,
    onIntent: (OnboardingIntent) -> Unit,
) {
    when (uiState.step) {
        OnboardingStep.Initializing,
        OnboardingStep.Completed,
        -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())

        OnboardingStep.PermissionGuide ->
            PermissionGuideContent(
                isConfirming = uiState.loadState.isLoading(OnboardingLoadKey.ConfirmPermissionGuide),
                onConfirm = { onIntent(OnboardingIntent.ConfirmPermissionGuide) },
            )

        OnboardingStep.Guide ->
            OnboardingGuideRoute(
                onCompleted = { onIntent(OnboardingIntent.ShopSelectionCompleted(it)) },
                analytics = analytics,
                toastManager = toastManager,
            )

        is OnboardingStep.Login ->
            OnboardingLoginRoute(
                isCompleting = uiState.loadState.isLoading(OnboardingLoadKey.Complete),
                onCompleted = { onIntent(OnboardingIntent.LoginStepCompleted) },
                toastManager = toastManager,
            )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingRoutePermissionGuidePreview() {
    RamapTheme {
        OnboardingRouteContent(
            uiState = OnboardingUiState(step = OnboardingStep.PermissionGuide),
            analytics =
                OnboardingAnalytics(
                    object : AnalyticsTracker {
                        override fun logEvent(event: AnalyticsEvent) {}

                        override fun logScreenView(
                            screenName: String,
                            params: Map<String, Any>,
                        ) {}

                        override fun userProperty(
                            key: String,
                            value: String?,
                        ) {}
                    },
                ),
            toastManager = ToastManager(),
            onIntent = {},
        )
    }
}
