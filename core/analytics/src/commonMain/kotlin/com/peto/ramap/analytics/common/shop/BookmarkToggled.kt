package com.peto.ramap.analytics.common.shop

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsSource

data class BookmarkToggled(
    val shopId: String,
    val shopName: String,
    val enabled: Boolean,
    val source: AnalyticsSource,
) : AnalyticsEvent {
    override val name: String = "bookmark_toggle"

    override fun params(): Map<String, Any> =
        mapOf(
            "shop_id" to shopId,
            "shop_name" to shopName,
            "enabled" to enabled,
            "source" to source.value,
        )
}
