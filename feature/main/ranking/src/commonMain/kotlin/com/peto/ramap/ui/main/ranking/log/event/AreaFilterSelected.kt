package com.peto.ramap.ui.main.ranking.log.event

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsSource

internal data class AreaFilterSelected(
    val area: String,
) : AnalyticsEvent {
    override val name: String = "area_filter_select"

    override fun params(): Map<String, Any> =
        mapOf(
            "area" to area,
            "source" to AnalyticsSource.RANKING.value,
        )
}
