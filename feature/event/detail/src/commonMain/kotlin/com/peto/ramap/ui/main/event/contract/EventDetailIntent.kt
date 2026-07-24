package com.peto.ramap.ui.main.event.contract

import com.peto.ramap.ui.base.Intent

sealed interface EventDetailIntent : Intent {
    data class OnEntered(
        val eventId: String,
    ) : EventDetailIntent

    data class OnNotificationChanged(
        val enabled: Boolean,
    ) : EventDetailIntent

    data object OnNotificationPermissionGranted : EventDetailIntent

    data class OnVenueShopSelected(
        val shopId: String,
    ) : EventDetailIntent

    data class OnCollaboratorShopSelected(
        val shopId: String,
    ) : EventDetailIntent

    data object OnCollaboratorInstagramSelected : EventDetailIntent

    data object OnWaitingLinkSelected : EventDetailIntent

    data object OnSourceLinkSelected : EventDetailIntent
}
