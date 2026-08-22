package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.loading.LoadState as TaskLoadState

data class EventsUiState(
    val loadedEvents: List<ShopEvent> = emptyList(),
    val selectedFilter: EventFilter = EventFilter.EVENT,
    val summerLimitedEvents: List<ShopEvents> = emptyList(),
    val ongoingEvents: List<ShopEvents> = emptyList(),
    val upcomingEvents: List<ShopEvents> = emptyList(),
    val readEventIds: Set<String>? = null,
    val showError: Boolean = false,
    override val loadState: TaskLoadState = TaskLoadState(),
) : State,
    LoadableState<EventsUiState> {
    val isLoading: Boolean
        get() = loadState.isLoading(EventsLoadKey.Fetch)

    val isRefreshing: Boolean
        get() = loadState.isLoading(EventsLoadKey.Refresh)

    val isEmpty: Boolean
        get() =
            summerLimitedEvents.isEmpty() &&
                ongoingEvents.isEmpty() &&
                upcomingEvents.isEmpty()

    val unreadEventIds: Set<String>
        get() =
            readEventIds
                ?.let { readIds ->
                    loadedEvents.map(ShopEvent::id).filterNot(readIds::contains).toSet()
                }.orEmpty()

    override fun withLoadingState(loadState: TaskLoadState): EventsUiState = copy(loadState = loadState)
}
