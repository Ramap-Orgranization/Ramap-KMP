package com.peto.ramap.domain.model.shop

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

class ShopVisitTimeWindow(
    now: LocalDateTime,
) {
    val today: LocalDate = now.date
    val tomorrow: LocalDate = today.plus(1, DateTimeUnit.DAY)

    private val firstVisitTime: LocalDateTime =
        visitTimes
            .firstOrNull { it >= now.time }
            ?.let { LocalDateTime(today, it) }
            ?: LocalDateTime(tomorrow, visitTimes.first())

    val availableDates: List<LocalDate> =
        List(VISIT_DATE_COUNT) { offset -> today.plus(offset, DateTimeUnit.DAY) }
            .filter { it >= firstVisitTime.date }

    fun resolve(draft: LocalDateTime?): ShopHoursFilter.VisitTime {
        val lastVisitTime = LocalDateTime(availableDates.last(), visitTimes.last())
        val candidate = (draft ?: firstVisitTime).coerceIn(firstVisitTime, lastVisitTime)
        val nextTime = visitTimes.firstOrNull { it >= candidate.time }
        val dateTime =
            if (nextTime != null) {
                LocalDateTime(candidate.date, nextTime)
            } else {
                LocalDateTime(candidate.date.plus(1, DateTimeUnit.DAY), visitTimes.first())
            }
        return ShopHoursFilter.VisitTime(dateTime)
    }

    fun availableTimes(date: LocalDate): List<LocalTime> =
        when (date) {
            firstVisitTime.date -> visitTimes.filter { it >= firstVisitTime.time }
            in availableDates -> visitTimes
            else -> emptyList()
        }

    private companion object {
        const val VISIT_DATE_COUNT = 7
        const val FIRST_VISIT_HOUR = 8
        const val HOURS_PER_DAY = 24

        val visitTimes = (FIRST_VISIT_HOUR until HOURS_PER_DAY).map { hour -> LocalTime(hour, 0) }
    }
}
