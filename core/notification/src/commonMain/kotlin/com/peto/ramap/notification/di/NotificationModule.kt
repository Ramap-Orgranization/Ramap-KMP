package com.peto.ramap.notification.di

import com.peto.ramap.notification.NotificationDeepLinkParser
import com.peto.ramap.notification.NotificationLaunchDispatcher
import com.peto.ramap.notification.NotificationRegistry
import org.koin.dsl.module

val notificationModule =
    module {
        single { NotificationDeepLinkParser() }
        single { NotificationLaunchDispatcher(get()) }
        single { NotificationRegistry(get(), get(), get()) }
    }
