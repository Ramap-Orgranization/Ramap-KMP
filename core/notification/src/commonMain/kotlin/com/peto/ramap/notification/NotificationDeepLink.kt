package com.peto.ramap.notification

sealed interface NotificationDeepLink {
    data class Event(
        val eventId: String,
    ) : NotificationDeepLink
}
