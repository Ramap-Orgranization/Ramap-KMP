package com.peto.ramap.ui.main.event.calendar.di

import com.peto.ramap.ui.main.event.calendar.EventCalendarViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val eventCalendarModule =
    module {
        viewModelOf(::EventCalendarViewModel)
    }
