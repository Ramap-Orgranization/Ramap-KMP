package com.peto.ramap.ui.main.event.list.log

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.ui.main.event.list.log.event.EventSelected
import com.peto.ramap.ui.main.event.list.log.event.EventStatus

class EventsAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logEventSelected(event: ShopEvent) {
        analyticsTracker.logEvent(
            EventSelected(
                eventId = event.id,
                status = eventStatus(event),
            ),
        )
    }

    private fun eventStatus(event: ShopEvent): EventStatus =
        if (event.isToday) {
            EventStatus.ONGOING
        } else {
            EventStatus.UPCOMING
        }
}
