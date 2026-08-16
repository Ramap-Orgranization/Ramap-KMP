package com.peto.ramap.ui.main.event.detail.log

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.common.event.EventNotificationToggled
import com.peto.ramap.ui.main.event.detail.log.event.EventExternalLinkSelected
import com.peto.ramap.ui.main.event.detail.log.event.EventExternalLinkSource
import com.peto.ramap.ui.main.event.detail.log.event.EventShopSelected
import com.peto.ramap.ui.main.event.detail.log.event.EventShopSource

class EventDetailAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logNotificationToggled(
        eventId: String,
        enabled: Boolean,
    ) {
        analyticsTracker.logEvent(
            EventNotificationToggled(
                eventId = eventId,
                enabled = enabled,
                source = AnalyticsSource.EVENT_DETAIL,
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
            source = EventShopSource.VENUE,
        )
    }

    fun logCollaboratorShopSelected(
        eventId: String,
        shopId: String,
    ) {
        logShopSelected(
            eventId = eventId,
            shopId = shopId,
            source = EventShopSource.COLLABORATOR,
        )
    }

    fun logCollaboratorInstagramSelected(eventId: String) {
        logExternalLinkSelected(
            eventId = eventId,
            source = EventExternalLinkSource.COLLABORATOR_INSTAGRAM,
        )
    }

    fun logWaitingLinkSelected(eventId: String) {
        logExternalLinkSelected(
            eventId = eventId,
            source = EventExternalLinkSource.WAITING,
        )
    }

    fun logSourceLinkSelected(eventId: String) {
        logExternalLinkSelected(
            eventId = eventId,
            source = EventExternalLinkSource.EVENT_SOURCE,
        )
    }

    private fun logShopSelected(
        eventId: String,
        shopId: String,
        source: EventShopSource,
    ) {
        analyticsTracker.logEvent(
            EventShopSelected(
                eventId = eventId,
                shopId = shopId,
                source = source,
            ),
        )
    }

    private fun logExternalLinkSelected(
        eventId: String,
        source: EventExternalLinkSource,
    ) {
        analyticsTracker.logEvent(
            EventExternalLinkSelected(
                eventId = eventId,
                source = source,
            ),
        )
    }
}
