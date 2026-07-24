package com.peto.ramap.ui.main.event.list

import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.domain.model.event.ShopEvent

class EventsAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logEventSelected(event: ShopEvent) {
        analyticsTracker.logEvent(
            name = AnalyticsEvents.EVENT_SELECT,
            params =
                mapOf(
                    AnalyticsParams.EVENT_ID to event.id,
                    AnalyticsParams.EVENT_STATUS to eventStatus(event),
                ),
        )
    }

    private fun eventStatus(event: ShopEvent): String =
        if (event.isToday) {
            EVENT_STATUS_ONGOING
        } else {
            EVENT_STATUS_UPCOMING
        }

    companion object {
        private const val EVENT_STATUS_ONGOING = "ongoing"
        private const val EVENT_STATUS_UPCOMING = "upcoming"
    }
}
