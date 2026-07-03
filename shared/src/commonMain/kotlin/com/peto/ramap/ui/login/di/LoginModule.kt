package com.peto.ramap.ui.login.di

import com.peto.ramap.ui.login.LoginViewModel
import org.koin.dsl.module

val loginModule =
    module {
        factory {
            LoginViewModel(
                loginRepository = get(),
            )
        }
    }
