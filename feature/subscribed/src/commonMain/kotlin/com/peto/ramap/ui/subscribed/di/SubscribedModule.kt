package com.peto.ramap.ui.subscribed.di

import com.peto.ramap.ui.subscribed.SubscribedShopListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val subscribedModule =
    module {
        viewModelOf(::SubscribedShopListViewModel)
    }
