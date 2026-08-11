package com.peto.ramap.ui.main.event.calendar.model

data class CalendarDaySelection(
    val days: List<CalendarDayEvents>,
    val initialIndex: Int,
)
