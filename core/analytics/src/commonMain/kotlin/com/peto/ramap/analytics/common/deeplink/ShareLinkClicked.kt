package com.peto.ramap.analytics.common.deeplink

import com.peto.ramap.analytics.AnalyticsEvent

data class ShareLinkClicked(
    val shopId: String,
) : AnalyticsEvent {
    override val name: String = "share_link_clicked"

    override fun params(): Map<String, Any> =
        mapOf(
            "link_type" to DeepLinkAnalyticsEvent.LINK_TYPE_SHOP,
            "shop_id" to shopId,
        )
}
