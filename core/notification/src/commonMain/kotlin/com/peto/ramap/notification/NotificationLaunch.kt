package com.peto.ramap.notification

data class NotificationLaunch(
    val notificationId: String,
    val destination: NotificationDeepLink,
)
