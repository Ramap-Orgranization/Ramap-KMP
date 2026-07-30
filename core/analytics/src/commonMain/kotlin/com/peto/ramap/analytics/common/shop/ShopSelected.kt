package com.peto.ramap.analytics.common.shop

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsSource

data class ShopSelected(
    val shopId: String,
    val shopName: String,
    val source: AnalyticsSource,
) : AnalyticsEvent {
    override val name: String = "shop_select"

    override fun params(): Map<String, Any> =
        buildMap {
            put("shop_id", shopId)
            put("shop_name", shopName)
            put("source", source.value)
        }
}
