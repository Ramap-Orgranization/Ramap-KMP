package com.peto.ramap.ui.announcement.di

import com.peto.ramap.ui.announcement.AnnouncementViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val announcementModule =
    module {
        viewModelOf(::AnnouncementViewModel)
    }
