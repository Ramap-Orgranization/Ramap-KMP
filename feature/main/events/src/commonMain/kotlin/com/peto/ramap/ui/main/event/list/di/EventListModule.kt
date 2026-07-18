package com.peto.ramap.ui.main.event.list.di

import com.peto.ramap.ui.main.event.list.EventsViewModel
import org.koin.dsl.module

val eventsModule =
    module {
        factory { EventsViewModel(get()) }
    }
