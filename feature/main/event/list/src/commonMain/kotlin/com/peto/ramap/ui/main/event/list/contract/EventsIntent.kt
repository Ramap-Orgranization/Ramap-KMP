package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.ui.base.Intent

sealed interface EventsIntent : Intent {
    data object OnEventsRefreshed : EventsIntent

    data object OnEventsRetried : EventsIntent

    data class OnEventClicked(
        val event: ShopEvent,
    ) : EventsIntent

    data class OnQueryChanged(
        val query: String,
    ) : EventsIntent

    data object OnSavedToggled : EventsIntent

    data object OnPersonalizationRetried : EventsIntent
}
