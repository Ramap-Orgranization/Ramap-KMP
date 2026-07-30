package com.peto.ramap.ui.main.map.di

import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.main.map.MapViewModel
import com.peto.ramap.ui.main.map.log.MapAnalytics
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mapModule =
    module {
        single { CurrentLocationStore() }
        viewModelOf(::MapViewModel)
        singleOf(::MapAnalytics)
    }
