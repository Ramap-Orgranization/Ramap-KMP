package com.peto.ramap.ui.hidden.di

import com.peto.ramap.ui.hidden.HiddenShopListAnalytics
import com.peto.ramap.ui.hidden.HiddenShopListViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val hiddenModule =
    module {
        viewModelOf(::HiddenShopListViewModel)
        singleOf(::HiddenShopListAnalytics)
    }
