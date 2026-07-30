package com.peto.ramap

import com.peto.ramap.notification.NotificationRegistry
import org.koin.mp.KoinPlatformTools

fun trackIosPushToken(token: String) {
    val registry = KoinPlatformTools.defaultContext().get().get<NotificationRegistry>()
    registry.track(
        identifier = token,
        platform = IOS_PLATFORM,
        targetType = TOKEN_TARGET_TYPE,
    )
}

private const val IOS_PLATFORM = "ios"
private const val TOKEN_TARGET_TYPE = "token"
