package com.peto.ramap.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.contract.OnboardingUiState
import com.peto.ramap.ui.onboarding.guide.OnboardingGuideRoute
import com.peto.ramap.ui.onboarding.guide.log.OnboardingAnalyticsStage
import com.peto.ramap.ui.onboarding.log.OnboardingAnalytics
import com.peto.ramap.ui.onboarding.permission.PermissionGuideRoute
import com.peto.ramap.ui.onboarding.permission.contract.OnboardingPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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

    OnboardingRouteContent(
        uiState = uiState,
        analytics = analytics,
        toastManager = toastManager,
        requestPermissionFlow = requestPermissionFlow,
        onIntent = viewModel::dispatch,
    )
}

@Composable
internal fun OnboardingRouteContent(
    uiState: OnboardingUiState,
    analytics: OnboardingAnalytics,
    toastManager: ToastManager,
    requestPermissionFlow: Flow<OnboardingPermission>,
    onIntent: (OnboardingIntent) -> Unit,
) {
    when (uiState.step) {
        OnboardingStep.Initializing,
        OnboardingStep.Completed,
        -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())

        OnboardingStep.PermissionGuide ->
            PermissionGuideRoute(
                isConfirming = uiState.loadState.isLoading(OnboardingLoadKey.ConfirmPermissionGuide),
                onRequestPermission = requestPermissionFlow,
                onConfirm = { onIntent(OnboardingIntent.ConfirmPermissionGuide) },
                onLocationPermissionFinished = { onIntent(OnboardingIntent.LocationPermissionFinished(it)) },
                onNotificationPermissionFinished = { onIntent(OnboardingIntent.NotificationPermissionFinished(it)) },
            )

        OnboardingStep.Guide ->
            OnboardingGuideRoute(
                onCompleted = { onIntent(OnboardingIntent.ShopSelectionCompleted) },
                analytics = analytics,
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
            requestPermissionFlow = emptyFlow(),
            onIntent = {},
        )
    }
}
