package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents

internal fun mapEventsToUiState(
    state: EventsUiState,
    events: List<ShopEvent>,
): EventsUiState = mapEventsToUiState(state.copy(loadedEvents = events))

internal fun mapEventsToUiState(state: EventsUiState): EventsUiState {
    val (ongoingEvents, upcomingEvents) = partitionBySchedule(state.loadedEvents)
    val summerLimitedEvents = ongoingEvents.filter { it.type == ShopEventType.SUMMER_LIMITED }
    val regularOngoingEvents = ongoingEvents.filterNot { it.type == ShopEventType.SUMMER_LIMITED }

    return state.copy(
        summerLimitedEvents = ShopEvents.groupByEventListShop(summerLimitedEvents),
        ongoingEvents = ShopEvents.groupByEventListShop(regularOngoingEvents),
        upcomingEvents = ShopEvents.groupByEventListShop(upcomingEvents),
    )
}

internal fun partitionBySchedule(events: List<ShopEvent>): Pair<List<ShopEvent>, List<ShopEvent>> = events.partition(ShopEvent::isToday)
