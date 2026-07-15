package com.peto.ramap.ui.settings.notification.contract

sealed interface NotificationRemovalTarget {
    data class Shop(
        val shopId: String,
    ) : NotificationRemovalTarget

    data class EventOverride(
        val eventId: String,
    ) : NotificationRemovalTarget
}
