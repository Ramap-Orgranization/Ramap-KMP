package com.peto.ramap.domain.model.event

data class ShopEvents(
    private val events: List<ShopEvent>,
) : List<ShopEvent> by events {
    init {
        require(events.isNotEmpty())
    }

    val representativeEvent: ShopEvent
        get() = events.first()

    val eventCount: Int
        get() = events.size

    val hasMultipleEvents: Boolean
        get() = events.size > 1

    companion object {
        fun groupByVenue(events: List<ShopEvent>): List<ShopEvents> =
            events
                .groupBy(ShopEvent::venueShopId)
                .values
                .map(::ShopEvents)
    }
}
