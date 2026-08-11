package com.peto.ramap.ui.main.event.di

import com.peto.ramap.ui.main.event.EventDetailViewModel
import com.peto.ramap.ui.main.event.log.EventDetailAnalytics
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val eventDetailModule =
    module {
        viewModelOf(::EventDetailViewModel)
        singleOf(::EventDetailAnalytics)
    }
