package com.peto.ramap.ui.main.map.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data class ShopShared(
    val shopId: String,
    val shopName: String,
) : AnalyticsEvent {
    override val name: String = "shop_share"

    override fun params(): Map<String, Any> =
        mapOf(
            "shop_id" to shopId,
            "shop_name" to shopName,
        )
}
