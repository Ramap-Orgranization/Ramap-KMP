package com.peto.ramap.ui.di

import com.peto.ramap.domain.usecase.SignupConsentRequester
import com.peto.ramap.ui.login.LoginGuideController
import com.peto.ramap.ui.login.SignupConsentController
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val uiModule =
    module {
        singleOf(::LoginGuideController)
        singleOf(::SignupConsentController) bind SignupConsentRequester::class
    }
