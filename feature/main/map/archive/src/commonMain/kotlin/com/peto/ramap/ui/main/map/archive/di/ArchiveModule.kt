package com.peto.ramap.ui.main.map.archive.di

import com.peto.ramap.ui.main.map.archive.ArchiveMapViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val archiveModule =
    module {
        viewModelOf(::ArchiveMapViewModel)
    }
