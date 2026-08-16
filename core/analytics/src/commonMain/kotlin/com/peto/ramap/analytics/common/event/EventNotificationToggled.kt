package com.peto.ramap.analytics.common.event

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsSource

data class EventNotificationToggled(
    val eventId: String?,
    val enabled: Boolean,
    val source: AnalyticsSource,
) : AnalyticsEvent {
    override val name: String = "event_notification_toggle"

    override fun params(): Map<String, Any> =
        buildMap {
            eventId?.let { put("event_id", it) }
            put("enabled", enabled)
            put("source", source.value)
        }
}
