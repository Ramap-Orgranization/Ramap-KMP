package com.peto.ramap.ui.main.event.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.ui.base.Intent

sealed interface EventDetailIntent : Intent {
    data class OnEntered(
        val eventId: String,
        val initialEvent: ShopEvent?,
    ) : EventDetailIntent

    data class OnNotificationChanged(
        val enabled: Boolean,
    ) : EventDetailIntent
}
