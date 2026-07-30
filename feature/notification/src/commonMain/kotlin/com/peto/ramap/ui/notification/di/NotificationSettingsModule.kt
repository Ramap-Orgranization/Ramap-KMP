package com.peto.ramap.ui.notification.di

import com.peto.ramap.ui.notification.NotificationSettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val notificationSettingsModule =
    module {
        viewModelOf(::NotificationSettingsViewModel)
    }
