package com.peto.ramap.ui.main.event.calendar

import com.peto.ramap.ui.main.event.calendar.model.CalendarMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class EventCalendarLogicTest {
    @Test
    fun `윤년과 월 경계를 처리한다`() {
        assertEquals(29, CalendarMonth(2024, 2).days().size)
        assertEquals(31, CalendarMonth(2025, 1).previous().days().size)
        assertEquals(CalendarMonth(2025, 1), CalendarMonth(2024, 12).next())
    }
}
