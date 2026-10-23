package com.peto.ramap.ui.di

import com.peto.ramap.ui.login.LoginGuideController
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val uiModule =
    module {
        singleOf(::LoginGuideController)
    }
