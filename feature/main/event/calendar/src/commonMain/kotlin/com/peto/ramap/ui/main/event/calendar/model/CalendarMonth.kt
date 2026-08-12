package com.peto.ramap.ui.main.event.calendar.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class CalendarMonth(
    val year: Int,
    val monthNumber: Int,
) {
    init {
        require(monthNumber in 1..12)
    }

    fun previous(): CalendarMonth = if (monthNumber == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, monthNumber - 1)

    fun next(): CalendarMonth = if (monthNumber == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, monthNumber + 1)

    fun days(): List<LocalDate> {
        val firstDay = LocalDate(year, monthNumber, 1)
        val nextMonthFirstDay = next().firstDay()
        return List(firstDay.daysUntil(nextMonthFirstDay)) { dayOffset ->
            firstDay.plus(dayOffset, DateTimeUnit.DAY)
        }
    }

    fun leadingEmptyCellCount(): Int = (firstDay().dayOfWeek.ordinal + 1) % DAYS_PER_WEEK

    fun firstDay(): LocalDate = LocalDate(year, monthNumber, 1)

    companion object {
        private const val DAYS_PER_WEEK = 7

        fun currentMonth(): CalendarMonth {
            val date = Clock.System.todayIn(TimeZone.currentSystemDefault())
            return CalendarMonth(date.year, date.month.ordinal + 1)
        }
    }
}
