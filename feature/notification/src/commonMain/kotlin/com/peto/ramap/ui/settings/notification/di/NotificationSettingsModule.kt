package com.peto.ramap.ui.settings.notification.di

import com.peto.ramap.ui.settings.notification.NotificationSettingsViewModel
import org.koin.dsl.module

val notificationSettingsModule =
    module {
        factory { NotificationSettingsViewModel(get(), get()) }
    }
