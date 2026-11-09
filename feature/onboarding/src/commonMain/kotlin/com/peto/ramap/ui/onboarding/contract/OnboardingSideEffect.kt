package com.peto.ramap.ui.onboarding.contract

import com.peto.ramap.ui.base.SideEffect

internal sealed interface OnboardingSideEffect : SideEffect {
    data class RequestPermission(
        val permission: OnboardingPermission,
    ) : OnboardingSideEffect
}
