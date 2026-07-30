package com.peto.ramap.ui.main.my.di

import com.peto.ramap.ui.main.my.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsModule =
    module {
        viewModelOf(::SettingsViewModel)
    }
