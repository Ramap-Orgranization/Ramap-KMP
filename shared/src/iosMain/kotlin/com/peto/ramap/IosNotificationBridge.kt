package com.peto.ramap

import com.peto.ramap.notification.NotificationLaunchDispatcher
import com.peto.ramap.notification.NotificationRegistry
import org.koin.mp.KoinPlatformTools

fun trackIosPushToken(token: String) {
    KoinPlatformTools.defaultContext().get().get<NotificationRegistry>().track(
        identifier = token,
        platform = "ios",
        targetType = "token",
    )
}

fun dispatchNotificationDeepLink(rawUrl: String?) {
    KoinPlatformTools
        .defaultContext()
        .get()
        .get<NotificationLaunchDispatcher>()
        .dispatch(rawUrl)
}
