package com.peto.ramap.ui.onboarding.login.di

import com.peto.ramap.ui.onboarding.login.OnboardingLoginViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingLoginModule =
    module {
        viewModelOf(::OnboardingLoginViewModel)
    }
