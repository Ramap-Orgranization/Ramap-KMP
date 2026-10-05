package com.peto.ramap.notification

import org.koin.mp.KoinPlatformTools

/** Registers an FCM registration token for delivery to this iOS installation. */
fun registerIosPushToken(token: String) {
    KoinPlatformTools.defaultContext().get().get<NotificationRegistry>().track(
        identifier = token,
        platform = PLATFORM_IOS,
        targetType = TARGET_TYPE_TOKEN,
    )
}

/** Passes a notification destination to the shared navigation observer. */
fun dispatchIosNotificationDeepLink(deepLink: String?) {
    KoinPlatformTools
        .defaultContext()
        .get()
        .get<NotificationLaunchDispatcher>()
        .dispatch(deepLink)
}

private const val PLATFORM_IOS = "ios"
private const val TARGET_TYPE_TOKEN = "token"
