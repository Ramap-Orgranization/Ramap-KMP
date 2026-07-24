package com.peto.ramap.ui.main.event.di

import com.peto.ramap.ui.main.event.EventDetailViewModel
import org.koin.dsl.module

val eventDetailModule =
    module {
        factory { EventDetailViewModel(get(), get(), get(), get()) }
    }
