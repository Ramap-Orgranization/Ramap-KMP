package com.peto.ramap.ui.subscribed.model

sealed interface SubscribedRemovalTarget {
    data class Shop(
        val shopId: String,
    ) : SubscribedRemovalTarget

    data class EventOverride(
        val eventId: String,
    ) : SubscribedRemovalTarget
}
