package com.peto.ramap.ui.main.map.di

import com.peto.ramap.ui.common.CurrentLocationStore
import com.peto.ramap.ui.main.map.MapViewModel
import org.koin.dsl.module

val mapModule =
    module {
        single { CurrentLocationStore() }
        factory {
            MapViewModel(get(), get(), get(), get(), get(), get(), get())
        }
    }
