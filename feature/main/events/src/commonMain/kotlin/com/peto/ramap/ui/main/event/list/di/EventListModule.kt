package com.peto.ramap.ui.main.event.list.di

import com.peto.ramap.ui.main.event.list.EventListViewModel
import org.koin.dsl.module

val eventListModule =
    module {
        factory { EventListViewModel(get()) }
    }
