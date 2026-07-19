package com.peto.ramap.ui.subscribed.di

import com.peto.ramap.ui.subscribed.SubscribedShopListViewModel
import org.koin.dsl.module

val subscribedModule =
    module {
        factory { SubscribedShopListViewModel(get(), get(), get()) }
    }
