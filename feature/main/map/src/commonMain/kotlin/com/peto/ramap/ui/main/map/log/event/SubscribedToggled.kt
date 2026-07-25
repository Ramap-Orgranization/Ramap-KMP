package com.peto.ramap.ui.main.map.log.event

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsSource

internal data class SubscribedToggled(
    val shopId: String,
    val shopName: String,
    val enabled: Boolean,
    val source: AnalyticsSource,
) : AnalyticsEvent {
    override val name: String = "subscribed_toggle"

    override fun params(): Map<String, Any> =
        mapOf(
            "shop_id" to shopId,
            "shop_name" to shopName,
            "enabled" to enabled,
            "source" to source.value,
        )
}
