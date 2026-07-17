package com.peto.ramap.ui.hidden.di

import com.peto.ramap.ui.hidden.HiddenShopListViewModel
import org.koin.dsl.module

val hiddenModule =
    module {
        factory {
            HiddenShopListViewModel(get(), get())
        }
    }
