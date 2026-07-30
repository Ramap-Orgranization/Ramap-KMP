package com.peto.ramap.ui.main.event.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data class EventNotificationToggled(
    val eventId: String,
    val enabled: Boolean,
) : AnalyticsEvent {
    override val name: String = "event_notification_toggle"

    override fun params(): Map<String, Any> =
        mapOf(
            "event_id" to eventId,
            "enabled" to enabled,
        )
}
