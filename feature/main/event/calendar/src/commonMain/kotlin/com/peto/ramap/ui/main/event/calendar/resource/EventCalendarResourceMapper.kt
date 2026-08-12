package com.peto.ramap.ui.main.event.calendar.resource

import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_calendar_weekday_friday
import ramap.shared.generated.resources.event_calendar_weekday_monday
import ramap.shared.generated.resources.event_calendar_weekday_saturday
import ramap.shared.generated.resources.event_calendar_weekday_sunday
import ramap.shared.generated.resources.event_calendar_weekday_thursday
import ramap.shared.generated.resources.event_calendar_weekday_tuesday
import ramap.shared.generated.resources.event_calendar_weekday_wednesday

object EventCalendarResourceMapper {
    fun weekdayLabel(dayOfWeek: DayOfWeek): StringResource =
        when (dayOfWeek) {
            DayOfWeek.SUNDAY -> Res.string.event_calendar_weekday_sunday
            DayOfWeek.MONDAY -> Res.string.event_calendar_weekday_monday
            DayOfWeek.TUESDAY -> Res.string.event_calendar_weekday_tuesday
            DayOfWeek.WEDNESDAY -> Res.string.event_calendar_weekday_wednesday
            DayOfWeek.THURSDAY -> Res.string.event_calendar_weekday_thursday
            DayOfWeek.FRIDAY -> Res.string.event_calendar_weekday_friday
            DayOfWeek.SATURDAY -> Res.string.event_calendar_weekday_saturday
        }
}
