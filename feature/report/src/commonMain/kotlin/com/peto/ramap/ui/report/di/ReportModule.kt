package com.peto.ramap.ui.report.di

import com.peto.ramap.ui.report.PlaceReportViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportModule =
    module {
        viewModelOf(::PlaceReportViewModel)
    }
