package com.peto.ramap.ui.main.event

import com.peto.ramap.analytics.AnalyticsEvents
import com.peto.ramap.analytics.AnalyticsParams
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.domain.model.event.ShopEvent

class EventDetailAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logDetailViewed(event: ShopEvent) {
        analyticsTracker.logEvent(
            AnalyticsEvents.EVENT_DETAIL_VIEW,
            mapOf(
                AnalyticsParams.EVENT_ID to event.id,
                AnalyticsParams.EVENT_TYPE to event.type.name.lowercase(),
                AnalyticsParams.VENUE_SHOP_ID to event.venueShopId,
                AnalyticsParams.IS_TODAY to event.isToday,
            ),
        )
    }

    fun logEventUnavailable(eventId: String) {
        analyticsTracker.logEvent(
            AnalyticsEvents.EVENT_UNAVAILABLE,
            mapOf(
                AnalyticsParams.EVENT_ID to eventId,
            ),
        )
    }

    fun logNotificationChanged(
        eventId: String,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            AnalyticsEvents.EVENT_NOTIFICATION_TOGGLE,
            mapOf(
                AnalyticsParams.EVENT_ID to eventId,
                AnalyticsParams.ENABLED to enabled,
            ),
        )
    }

    fun logVenueShopSelected(
        eventId: String,
        shopId: String,
    ) {
        logShopSelected(
            eventId = eventId,
            shopId = shopId,
            source = VENUE_SOURCE,
        )
    }

    fun logCollaboratorShopSelected(
        eventId: String,
        shopId: String,
    ) {
        logShopSelected(
            eventId = eventId,
            shopId = shopId,
            source = COLLABORATOR_SOURCE,
        )
    }

    fun logCollaboratorInstagramSelected(eventId: String) {
        analyticsTracker.logEvent(
            AnalyticsEvents.EVENT_EXTERNAL_LINK_SELECT,
            mapOf(
                AnalyticsParams.EVENT_ID to eventId,
                AnalyticsParams.SOURCE to COLLABORATOR_INSTAGRAM_SOURCE,
            ),
        )
    }

    fun logWaitingLinkSelected(eventId: String) {
        analyticsTracker.logEvent(
            AnalyticsEvents.EVENT_EXTERNAL_LINK_SELECT,
            mapOf(
                AnalyticsParams.EVENT_ID to eventId,
                AnalyticsParams.SOURCE to WAITING_SOURCE,
            ),
        )
    }

    fun logSourceLinkSelected(eventId: String) {
        analyticsTracker.logEvent(
            AnalyticsEvents.EVENT_EXTERNAL_LINK_SELECT,
            mapOf(
                AnalyticsParams.EVENT_ID to eventId,
                AnalyticsParams.SOURCE to EVENT_SOURCE,
            ),
        )
    }

    private fun logShopSelected(
        eventId: String,
        shopId: String,
        source: String,
    ) {
        analyticsTracker.logEvent(
            AnalyticsEvents.EVENT_SHOP_SELECT,
            mapOf(
                AnalyticsParams.EVENT_ID to eventId,
                AnalyticsParams.SHOP_ID to shopId,
                AnalyticsParams.SOURCE to source,
            ),
        )
    }

    companion object {
        private const val VENUE_SOURCE = "venue"
        private const val COLLABORATOR_SOURCE = "collaborator"
        private const val COLLABORATOR_INSTAGRAM_SOURCE = "collaborator_instagram"
        private const val WAITING_SOURCE = "waiting"
        private const val EVENT_SOURCE = "event_source"
    }
}
