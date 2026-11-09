package com.peto.ramap.ui.onboarding.contract

import com.peto.ramap.platform.permission.NotificationPermissionOutcome
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.ui.base.Intent

sealed interface OnboardingIntent : Intent {
    data object ConfirmPermissionGuide : OnboardingIntent

    data object ShopSelectionCompleted : OnboardingIntent

    data class LocationPermissionFinished(
        val status: PermissionStatus,
    ) : OnboardingIntent

    data class NotificationPermissionFinished(
        val outcome: NotificationPermissionOutcome,
    ) : OnboardingIntent
}
