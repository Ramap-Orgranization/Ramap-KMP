package com.peto.ramap.ui.main.event.calendar.model

import com.peto.ramap.domain.model.event.ShopEvent
import kotlinx.datetime.LocalDate

data class CalendarDayEvents(
    val date: LocalDate,
    val events: List<ShopEvent>,
)
