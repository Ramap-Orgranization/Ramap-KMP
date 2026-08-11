package com.peto.ramap.ui.main.event.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data class EventExternalLinkSelected(
    val eventId: String,
    val source: EventExternalLinkSource,
) : AnalyticsEvent {
    override val name: String = "event_external_link_select"

    override fun params(): Map<String, Any> =
        mapOf(
            "event_id" to eventId,
            "source" to source.value,
        )
}
