package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents

internal fun mapEventsToUiState(
    state: EventsUiState,
    events: List<ShopEvent>,
): EventsUiState = mapEventsToUiState(state.copy(loadedEvents = events))

internal fun mapEventsToUiState(state: EventsUiState): EventsUiState {
    val visibleEvents = state.loadedEvents.filter { state.selectedFilter.matches(it) }
    val (ongoingEvents, upcomingEvents) = partitionBySchedule(visibleEvents)
    val summerLimitedEvents = ongoingEvents.filter { it.type == ShopEventType.SUMMER_LIMITED }
    val regularOngoingEvents = ongoingEvents.filterNot { it.type == ShopEventType.SUMMER_LIMITED }

    return state.copy(
        summerLimitedEvents = ShopEvents.groupByVenue(summerLimitedEvents),
        ongoingEvents = ShopEvents.groupByVenue(regularOngoingEvents),
        upcomingEvents = ShopEvents.groupByVenue(upcomingEvents),
    )
}

internal fun selectEventFilter(
    state: EventsUiState,
    filter: EventFilter,
): EventsUiState = mapEventsToUiState(state.copy(selectedFilter = filter))

internal fun partitionBySchedule(events: List<ShopEvent>): Pair<List<ShopEvent>, List<ShopEvent>> = events.partition(ShopEvent::isToday)
