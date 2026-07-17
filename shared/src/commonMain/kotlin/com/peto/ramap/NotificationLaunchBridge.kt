package com.peto.ramap

import com.peto.ramap.notification.NotificationLaunchDispatcher
import org.koin.mp.KoinPlatform

fun dispatchNotificationDeepLink(deepLink: String?) {
    KoinPlatform.getKoin().get<NotificationLaunchDispatcher>().dispatch(deepLink)
}
