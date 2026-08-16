package com.peto.ramap.ui.bookmark.importation.log

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.shop.BookmarkToggled
import com.peto.ramap.domain.model.importation.ImportationProvider
import com.peto.ramap.domain.model.shop.RamenShops
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

    fun logBookmarksAdded(shops: RamenShops) {
        shops.values.forEach { shop ->
            analyticsTracker.logEvent(
                BookmarkToggled(
                    shopId = shop.id,
                    shopName = shop.name,
                    enabled = true,
                    source = AnalyticsSource.IMPORTATION,
                ),
            )
        }
    }
}
