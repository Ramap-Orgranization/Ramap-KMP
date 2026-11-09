package com.peto.ramap.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.platform.permission.rememberNotificationPermissionGenerator
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingPermission
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
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
            toastManager = toastManager,
        )
    }
}
