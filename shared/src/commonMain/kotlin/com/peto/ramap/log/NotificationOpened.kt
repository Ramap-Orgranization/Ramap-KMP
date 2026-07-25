package com.peto.ramap.log

import com.peto.ramap.analytics.AnalyticsEvent

data class NotificationOpened(
    val eventId: String,
) : AnalyticsEvent {
    override val name: String = "notification_open"

    override fun params(): Map<String, Any> =
        mapOf(
            "event_id" to eventId,
        )
}
