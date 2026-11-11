package com.peto.ramap.notification

sealed interface NotificationDeepLink {
    data class Event(
        val eventId: String,
    ) : NotificationDeepLink

    data class OperatingNotice(
        val noticeId: String,
    ) : NotificationDeepLink

    data class Review(
        val reviewId: String,
    ) : NotificationDeepLink

    data object OperatingNotices : NotificationDeepLink

    data class ExternalUrl(
        val url: String,
    ) : NotificationDeepLink

    data object None : NotificationDeepLink
}
