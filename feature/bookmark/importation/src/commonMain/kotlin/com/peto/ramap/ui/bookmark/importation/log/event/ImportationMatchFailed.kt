package com.peto.ramap.ui.bookmark.importation.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data class ImportationMatchFailed(
    val provider: String,
    val placeName: String,
) : AnalyticsEvent {
    override val name: String = "importation_match_failed"

    override fun params(): Map<String, Any> =
        mapOf(
            "provider" to provider,
            "place_name" to placeName,
        )
}
