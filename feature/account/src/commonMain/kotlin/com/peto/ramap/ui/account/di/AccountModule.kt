package com.peto.ramap.ui.account.di

import com.peto.ramap.ui.account.AccountAnalytics
import com.peto.ramap.ui.account.AccountViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountModule =
    module {
        viewModelOf(::AccountViewModel)
        factory { AccountAnalytics(get()) }
    }
