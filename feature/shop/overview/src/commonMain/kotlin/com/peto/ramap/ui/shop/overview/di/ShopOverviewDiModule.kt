package com.peto.ramap.ui.shop.overview.di

import com.peto.ramap.ui.shop.overview.OverviewViewModel
import com.peto.ramap.ui.shop.overview.log.ShopAnalytics
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val shopOverviewModule =
    module {
        singleOf(::ShopAnalytics)
        viewModelOf(::OverviewViewModel)
    }
