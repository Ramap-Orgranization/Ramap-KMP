package com.peto.ramap.ui.onboarding.di

import com.peto.ramap.ui.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule =
    module {
        viewModelOf(::OnboardingViewModel)
    }
