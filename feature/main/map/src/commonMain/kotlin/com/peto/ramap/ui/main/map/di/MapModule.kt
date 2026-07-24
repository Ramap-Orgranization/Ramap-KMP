package com.peto.ramap.ui.main.map.di

import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.main.map.MapAnalytics
import com.peto.ramap.ui.main.map.MapViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mapModule =
    module {
        single { CurrentLocationStore() }
        viewModelOf(::MapViewModel)
        factory { MapAnalytics(get()) }
    }
