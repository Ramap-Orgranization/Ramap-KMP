package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.loading.LoadState as TaskLoadState

data class EventsUiState(
    val ongoingEvents: List<ShopEvents> = emptyList(),
    val upcomingEvents: List<ShopEvents> = emptyList(),
    val summerLimitedEvents: List<ShopEvents> = emptyList(),
    val showError: Boolean = false,
    override val loadState: TaskLoadState = TaskLoadState(),
) : State,
    LoadableState<EventsUiState> {
    val isLoading: Boolean
        get() = loadState.isLoading(EventsLoadKey.Fetch)

    val isRefreshing: Boolean
        get() = loadState.isLoading(EventsLoadKey.Refresh)

    override fun withLoadingState(loadState: TaskLoadState): EventsUiState = copy(loadState = loadState)
}
