package com.peto.ramap.ui.main.map.log.event

import com.peto.ramap.analytics.AnalyticsEvent

data class ShopMapLinkOpened(
    val shopId: String,
    val shopName: String,
    val mapProvider: String,
) : AnalyticsEvent {
    override val name: String = "shop_map_link_open"

    override fun params(): Map<String, Any> =
        mapOf(
            "shop_id" to shopId,
            "shop_name" to shopName,
            "map_provider" to mapProvider,
        )
}
