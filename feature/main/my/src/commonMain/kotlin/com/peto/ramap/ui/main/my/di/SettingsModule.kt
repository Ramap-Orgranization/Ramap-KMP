package com.peto.ramap.ui.main.my.di

import com.peto.ramap.ui.main.my.SettingsViewModel
import org.koin.dsl.module

val settingsModule =
    module {
        factory { SettingsViewModel(get()) }
    }
