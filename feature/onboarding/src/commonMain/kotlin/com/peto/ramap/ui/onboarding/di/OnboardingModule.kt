package com.peto.ramap.ui.onboarding.di

import com.peto.ramap.ui.onboarding.OnboardingViewModel
import com.peto.ramap.ui.onboarding.login.di.onboardingLoginModule
import com.peto.ramap.ui.onboarding.shopselection.ShopSelectionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingModule =
    module {
        includes(onboardingLoginModule)
        viewModelOf(::OnboardingViewModel)
        viewModelOf(::ShopSelectionViewModel)
    }
