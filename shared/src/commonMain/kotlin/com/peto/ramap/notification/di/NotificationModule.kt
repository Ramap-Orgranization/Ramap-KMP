package com.peto.ramap.notification.di

import com.peto.ramap.notification.NotificationRegistry
import org.koin.dsl.module

val notificationModule =
    module {
        single { NotificationDeepLinkParser() }
        single { NotificationRegistry(get(), get()) }
    }
