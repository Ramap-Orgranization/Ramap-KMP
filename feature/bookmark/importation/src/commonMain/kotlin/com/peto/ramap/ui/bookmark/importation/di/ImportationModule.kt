package com.peto.ramap.ui.bookmark.importation.di

import com.peto.ramap.ui.bookmark.importation.ImportationViewModel
import com.peto.ramap.ui.bookmark.importation.log.ImportationAnalytics
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val importationModule =
    module {
        singleOf(::ImportationAnalytics)
        viewModelOf(::ImportationViewModel)
    }
