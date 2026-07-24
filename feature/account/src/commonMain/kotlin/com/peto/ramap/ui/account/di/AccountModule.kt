package com.peto.ramap.ui.account.di

import com.peto.ramap.ui.account.AccountAnalytics
import com.peto.ramap.ui.account.AccountViewModel
import org.koin.dsl.module

val accountModule =
    module {
        factory { AccountViewModel(get(), get()) }
        factory { AccountAnalytics(get()) }
    }
