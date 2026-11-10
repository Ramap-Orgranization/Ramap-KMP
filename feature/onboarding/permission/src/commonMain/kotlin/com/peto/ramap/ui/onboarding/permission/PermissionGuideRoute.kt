package com.peto.ramap.ui.onboarding.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.peto.ramap.platform.permission.NotificationPermissionOutcome
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.permission.rememberLocationPermissionGenerator
import com.peto.ramap.platform.permission.rememberNotificationPermissionGenerator
import com.peto.ramap.ui.onboarding.permission.contract.OnboardingPermission
import kotlinx.coroutines.flow.Flow

@Composable
fun PermissionGuideRoute(
    isConfirming: Boolean,
    onRequestPermission: Flow<OnboardingPermission>,
    onConfirm: () -> Unit,
    onLocationPermissionFinished: (PermissionStatus) -> Unit,
    onNotificationPermissionFinished: (NotificationPermissionOutcome) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationPermission = rememberLocationPermissionGenerator(onResult = onLocationPermissionFinished)
    val notificationPermission = rememberNotificationPermissionGenerator(onResult = onNotificationPermissionFinished)

    LaunchedEffect(lifecycleOwner, locationPermission, notificationPermission) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            onRequestPermission.collect { permission ->
                when (permission) {
                    OnboardingPermission.Location -> locationPermission.requestPermission()
                    OnboardingPermission.Notification -> notificationPermission.requestPermission()
                }
            }
        }
    }

    PermissionGuideContent(
        isConfirming = isConfirming,
        onConfirm = onConfirm,
    )
}
