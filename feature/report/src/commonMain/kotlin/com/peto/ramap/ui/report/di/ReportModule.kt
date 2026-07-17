package com.peto.ramap.ui.report.di

import com.peto.ramap.ui.report.PlaceReportViewModel
import org.koin.dsl.module

val reportModule =
    module {
        factory { PlaceReportViewModel(get(), get(), get(), get()) }
    }
