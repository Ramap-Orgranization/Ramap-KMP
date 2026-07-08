package com.peto.ramap.ui.map.di

import com.peto.ramap.ui.map.MapViewModel
import org.koin.dsl.module

val mapModule =
    module {
        factory {
            MapViewModel(get(), get(), get(), get(), get())
        }
    }
