package com.peto.ramap.ui.main.event.contract

import com.peto.ramap.core.base.Intent
import com.peto.ramap.domain.model.ShopEvent

sealed interface EventDetailIntent : Intent {
    data class OnEntered(
        val eventId: String,
        val initialEvent: ShopEvent?,
    ) : EventDetailIntent

    data class OnNotificationChanged(
        val enabled: Boolean,
    ) : EventDetailIntent
}
