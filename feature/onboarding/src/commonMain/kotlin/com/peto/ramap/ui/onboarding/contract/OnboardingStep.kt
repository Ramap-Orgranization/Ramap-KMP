package com.peto.ramap.ui.onboarding.contract

sealed interface OnboardingStep {
    data object Initializing : OnboardingStep

    data object PermissionGuide : OnboardingStep

    data object Guide : OnboardingStep

    data class Login(
        val savedShopCount: Int,
    ) : OnboardingStep

    data object Completed : OnboardingStep
}
