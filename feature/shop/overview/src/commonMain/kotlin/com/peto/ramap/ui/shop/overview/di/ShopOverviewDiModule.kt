package com.peto.ramap.ui.shop.overview.di

import com.peto.ramap.ui.shop.log.ShopAnalytics
import com.peto.ramap.ui.shop.overview.ShopOverviewViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val shopOverviewModule =
    module {
        singleOf(::ShopAnalytics)
        viewModelOf(::ShopOverviewViewModel)
    }
