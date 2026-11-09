package com.peto.ramap.ui.onboarding.contract

import com.peto.ramap.ui.base.Intent

internal sealed interface OnboardingIntent : Intent {
    data object ConfirmPermissionGuide : OnboardingIntent

    data class PermissionRequestFinished(
        val permission: OnboardingPermission,
    ) : OnboardingIntent
}
