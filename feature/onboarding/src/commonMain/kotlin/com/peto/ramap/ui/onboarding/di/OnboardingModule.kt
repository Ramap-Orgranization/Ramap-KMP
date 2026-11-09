package com.peto.ramap.ui.onboarding.di

import com.peto.ramap.ui.onboarding.OnboardingViewModel
import com.peto.ramap.ui.onboarding.ShopSelectionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule =
    module {
        viewModelOf(::OnboardingViewModel)
        viewModelOf(::ShopSelectionViewModel)
    }
