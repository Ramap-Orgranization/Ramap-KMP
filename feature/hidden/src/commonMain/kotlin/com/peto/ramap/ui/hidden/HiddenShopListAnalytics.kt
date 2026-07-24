package com.peto.ramap.ui.hidden

import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsTracker

class HiddenShopListAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logUnhideConfirmed(shopId: String) {
        analyticsTracker.logEvent(
            AnalyticsEvents.HIDDEN_SHOP_UNHIDE,
            mapOf(
                AnalyticsParams.SHOP_ID to shopId,
            ),
        )
    }
}
