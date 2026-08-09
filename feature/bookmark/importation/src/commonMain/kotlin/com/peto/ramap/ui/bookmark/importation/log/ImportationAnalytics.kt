package com.peto.ramap.ui.bookmark.importation.log

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.domain.model.importation.ImportationProvider
import com.peto.ramap.ui.bookmark.importation.log.event.ImportationMatchFailed

class ImportationAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logMatchFailures(
        provider: ImportationProvider,
        placeNames: List<String>,
    ) {
        placeNames.forEach { placeName ->
            analyticsTracker.logEvent(
                ImportationMatchFailed(
                    provider = provider.name.lowercase(),
                    placeName = placeName,
                ),
            )
        }
    }
}
