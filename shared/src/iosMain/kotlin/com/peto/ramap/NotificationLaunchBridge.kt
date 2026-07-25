package com.peto.ramap

import com.peto.ramap.notification.NotificationLaunchDispatcher
import org.koin.mp.KoinPlatformTools

fun dispatchNotificationDeepLink(deepLink: String?) {
    val dispatcher = KoinPlatformTools.defaultContext().get().get<NotificationLaunchDispatcher>()
    dispatcher.dispatch(deepLink)
}
