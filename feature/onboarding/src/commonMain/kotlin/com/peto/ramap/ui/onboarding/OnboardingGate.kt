package com.peto.ramap.ui.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.platform.permission.rememberNotificationPermissionGenerator
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingPermission
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import kotlinx.coroutines.CancellationException
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingGate(content: @Composable () -> Unit) {
    val viewModel: OnboardingViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationPermission =
        rememberLocationPermissionGenerator {
            viewModel.dispatch(OnboardingIntent.PermissionRequestFinished(OnboardingPermission.Location))
        }
    val notificationPermission =
        rememberNotificationPermissionGenerator {
            viewModel.dispatch(OnboardingIntent.PermissionRequestFinished(OnboardingPermission.Notification))
        }

    LaunchedEffect(viewModel, lifecycleOwner, locationPermission, notificationPermission) {
        // 앞선 시스템 팝업이 닫히고 앱이 활성화된 뒤 다음 권한을 요청한다.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.sideEffect.collect { effect ->
                when (effect) {
                    is OnboardingSideEffect.RequestPermission ->
                        try {
                            when (effect.permission) {
                                OnboardingPermission.Location -> locationPermission.requestPermission()
                                OnboardingPermission.Notification -> notificationPermission.requestPermission()
                            }
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Exception) {
                            viewModel.dispatch(OnboardingIntent.PermissionRequestFinished(effect.permission))
                        }
                }
            }
        }
    }

    when {
        state.loadState.isLoading(OnboardingLoadKey.ReadConfirmation) -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
        state.hasCompletedOnboarding -> content()
        state.hasConfirmedPermissionGuide ->
            ShopSelectionRoute(onCompleted = { viewModel.dispatch(OnboardingIntent.ShopSelectionCompleted) })
        else ->
            PermissionGuideContent(
                isConfirming = state.loadState.isLoading(OnboardingLoadKey.ConfirmPermissionGuide),
                onConfirm = { viewModel.dispatch(OnboardingIntent.ConfirmPermissionGuide) },
            )
    }
}
