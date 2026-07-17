package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.ui.base.Intent

sealed interface EventListIntent : Intent {
    data object OnEventListEntered : EventListIntent

    data object OnEventListRefreshed : EventListIntent

    data object OnEventListRetried : EventListIntent
}
