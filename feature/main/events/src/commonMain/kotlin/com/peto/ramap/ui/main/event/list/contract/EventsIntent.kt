package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.ui.base.Intent

sealed interface EventsIntent : Intent {
    data object OnEventsRefreshed : EventsIntent

    data object OnEventsRetried : EventsIntent
}
