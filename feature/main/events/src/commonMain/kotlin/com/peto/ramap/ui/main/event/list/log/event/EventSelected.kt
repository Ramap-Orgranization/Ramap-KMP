package com.peto.ramap.ui.main.event.list.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data class EventSelected(
    val eventId: String,
    val status: EventStatus,
) : AnalyticsEvent {
    override val name: String = "event_select"

    override fun params(): Map<String, Any> =
        mapOf(
            "event_id" to eventId,
            "event_status" to status.value,
        )
}
