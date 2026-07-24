package com.peto.ramap.ui.bookmark

import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsTracker

class BookmarkedShopListAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logBookmarkRemovalConfirmed(shopId: String) {
        analyticsTracker.logEvent(
            AnalyticsEvents.BOOKMARKED_SHOP_REMOVE,
            mapOf(
                AnalyticsParams.SHOP_ID to shopId,
            ),
        )
    }
}
