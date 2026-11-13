package com.peto.ramap.ui.onboarding.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect
import com.peto.ramap.ui.onboarding.permission.contract.OnboardingPermission

sealed interface OnboardingSideEffect : SideEffect {
    data object Completed : OnboardingSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : OnboardingSideEffect

    data class RequestPermission(
        val permission: OnboardingPermission,
    ) : OnboardingSideEffect
}
