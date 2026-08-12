package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents

internal fun mapEventsToUiState(
    state: EventsUiState,
    events: List<ShopEvent>,
): EventsUiState {
    val summerLimitedEvents = events.filter { it.type == ShopEventType.SUMMER_LIMITED && it.isToday }
    val (ongoingEvents, upcomingEvents) = partitionBySchedule(events)

    return state.copy(
        ongoingEvents = ShopEvents.groupByVenue(ongoingEvents),
        upcomingEvents = ShopEvents.groupByVenue(upcomingEvents),
        summerLimitedEvents = ShopEvents.groupByVenue(summerLimitedEvents),
    )
}

internal fun partitionBySchedule(events: List<ShopEvent>): Pair<List<ShopEvent>, List<ShopEvent>> =
    events
        .filterNot { it.type == ShopEventType.SUMMER_LIMITED && it.isToday }
        .partition(ShopEvent::isToday)
