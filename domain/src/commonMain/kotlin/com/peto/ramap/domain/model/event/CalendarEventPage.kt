package com.peto.ramap.domain.model.event

data class CalendarEventPage(
    val events: List<ShopEvent>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
)
