package com.peto.ramap.domain.model.event

import kotlinx.datetime.LocalDate

data class CalendarEventPage(
    val events: List<ShopEvent>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val notificationDates: List<LocalDate> = emptyList(),
)
