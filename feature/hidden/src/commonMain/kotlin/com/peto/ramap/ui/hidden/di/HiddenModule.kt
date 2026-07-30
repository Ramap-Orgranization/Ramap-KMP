package com.peto.ramap.ui.hidden.di

import com.peto.ramap.ui.hidden.HiddenShopListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val hiddenModule =
    module {
        viewModelOf(::HiddenShopListViewModel)
    }
