package com.peto.ramap.ui.onboarding.login.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

internal sealed interface OnboardingLoginSideEffect : SideEffect {
    data object Completed : OnboardingLoginSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : OnboardingLoginSideEffect
}
