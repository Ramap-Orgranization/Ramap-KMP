package com.peto.ramap.analytics.di

import com.peto.ramap.analytics.common.login.LoginAnalytics
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val analyticsModule =
    module {
        singleOf(::LoginAnalytics)
    }
