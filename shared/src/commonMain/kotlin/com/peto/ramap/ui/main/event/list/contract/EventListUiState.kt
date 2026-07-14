package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.core.base.State
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.ui.common.LoadState

data class EventListUiState(
    val eventsState: LoadState<List<ShopEvent>> = LoadState.Idle,
    val isRefreshing: Boolean = false,
) : State
