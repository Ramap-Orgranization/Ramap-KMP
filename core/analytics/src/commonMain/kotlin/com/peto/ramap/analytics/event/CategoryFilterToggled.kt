package com.peto.ramap.analytics.event

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsSource

data class CategoryFilterToggled(
    val category: String,
    val enabled: Boolean,
    val source: AnalyticsSource,
) : AnalyticsEvent {
    override val name: String = "category_filter_toggle"

    override fun params(): Map<String, Any> =
        mapOf(
            "category" to category,
            "enabled" to enabled,
            "source" to source.value,
        )
}
