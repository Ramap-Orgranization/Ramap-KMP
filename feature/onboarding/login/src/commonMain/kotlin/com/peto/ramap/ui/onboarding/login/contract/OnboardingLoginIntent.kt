package com.peto.ramap.ui.onboarding.login.contract

import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.ui.base.Intent

internal sealed interface OnboardingLoginIntent : Intent {
    data class SelectLoginType(
        val type: LoginType,
    ) : OnboardingLoginIntent

    data object ContinueWithoutLogin : OnboardingLoginIntent
}
