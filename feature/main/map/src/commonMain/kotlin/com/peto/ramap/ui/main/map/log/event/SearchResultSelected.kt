package com.peto.ramap.ui.main.map.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data class SearchResultSelected(
    val shopName: String,
) : AnalyticsEvent {
    override val name: String = "search_result_select"

    override fun params(): Map<String, Any> =
        mapOf(
            "shop_name" to shopName,
        )
}
