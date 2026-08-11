package com.peto.ramap.ui.main.event.detail.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data class EventShopSelected(
    val eventId: String,
    val shopId: String,
    val source: EventShopSource,
) : AnalyticsEvent {
    override val name: String = "event_shop_select"

    override fun params(): Map<String, Any> =
        mapOf(
            "event_id" to eventId,
            "shop_id" to shopId,
            "source" to source.value,
        )
}
