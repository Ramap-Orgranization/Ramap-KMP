package com.peto.ramap.log

import com.peto.ramap.analytics.AnalyticsEvent

data class SharedShopLinkOpened(
    val shopId: String,
) : AnalyticsEvent {
    override val name: String = "shared_shop_link_open"

    override fun params(): Map<String, Any> = mapOf("shop_id" to shopId)
}
