package com.peto.ramap.domain.model.event

import kotlinx.datetime.LocalDate

fun groupShopEventsByDate(
    dates: List<LocalDate>,
    events: List<ShopEvent>,
): Map<LocalDate, List<ShopEvent>> =
    buildMap {
        dates.forEach { date ->
            events
                .filter { event -> event.occursOn(date) }
                .takeIf { it.isNotEmpty() }
                ?.let { dayEvents -> put(date, dayEvents) }
        }
    }
