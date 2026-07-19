package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.common.LoadState

data class EventsUiState(
    val eventsState: LoadState<List<ShopEvent>> = LoadState.Idle,
    val isRefreshing: Boolean = false,
) : State
