package com.peto.ramap.ui.notification.di

import com.peto.ramap.ui.notification.NotificationSettingsViewModel
import org.koin.dsl.module

val notificationSettingsModule =
    module {
        factory { NotificationSettingsViewModel(get()) }
    }
