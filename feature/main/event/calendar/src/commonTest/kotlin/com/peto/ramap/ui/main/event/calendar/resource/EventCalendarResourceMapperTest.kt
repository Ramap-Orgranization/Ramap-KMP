package com.peto.ramap.ui.main.event.calendar.resource

import kotlinx.datetime.DayOfWeek
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_calendar_weekday_monday
import ramap.shared.generated.resources.event_calendar_weekday_saturday
import ramap.shared.generated.resources.event_calendar_weekday_sunday
import kotlin.test.Test
import kotlin.test.assertEquals

class EventCalendarResourceMapperTest {
    @Test
    fun `요일을 캘린더 요일 리소스에 매핑한다`() {
        assertEquals(
            Res.string.event_calendar_weekday_sunday,
            EventCalendarResourceMapper.weekdayLabel(DayOfWeek.SUNDAY),
        )
        assertEquals(
            Res.string.event_calendar_weekday_monday,
            EventCalendarResourceMapper.weekdayLabel(DayOfWeek.MONDAY),
        )
        assertEquals(
            Res.string.event_calendar_weekday_saturday,
            EventCalendarResourceMapper.weekdayLabel(DayOfWeek.SATURDAY),
        )
    }
}
