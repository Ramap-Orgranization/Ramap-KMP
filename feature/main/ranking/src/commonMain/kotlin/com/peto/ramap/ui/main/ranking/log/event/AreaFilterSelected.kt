package com.peto.ramap.ui.main.ranking.log.event

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsSource

internal data class AreaFilterSelected(
    val area: String,
    val district: String?,
    val source: AnalyticsSource,
) : AnalyticsEvent {
    override val name: String = "area_filter_select"

    override fun params(): Map<String, Any> =
        buildMap {
            put("area", area)
            put("source", source.value)
            district?.let { put("sigungu", it) }
        }
}
