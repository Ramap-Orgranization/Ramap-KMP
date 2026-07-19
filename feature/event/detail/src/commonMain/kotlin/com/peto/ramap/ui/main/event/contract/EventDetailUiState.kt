package com.peto.ramap.ui.main.event.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.ui.base.State

data class EventDetailUiState(
    val event: ShopEvent? = null,
    val isNotificationVisible: Boolean = false,
    val isEventDayOnly: Boolean = false,
    val canChangeNotification: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val isNotificationLoading: Boolean = false,
) : State
