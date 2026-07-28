package com.peto.ramap.analytics.common.attribution

import com.peto.ramap.analytics.AnalyticsEvent

data class InstallAttributed(
    val clickId: String? = null,
    val source: String? = null,
    val campaign: String? = null,
    val shopId: String? = null,
) : AnalyticsEvent {
    override val name: String = "install_attributed"

    override fun params(): Map<String, Any> =
        buildMap {
            clickId?.let { put("click_id", it) }
            source?.let { put("source", it) }
            campaign?.let { put("campaign", it) }
            shopId?.let { put("shop_id", it) }
        }
}
