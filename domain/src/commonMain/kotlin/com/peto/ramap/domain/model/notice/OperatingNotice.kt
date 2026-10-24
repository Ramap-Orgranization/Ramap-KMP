package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursScheduleOverride
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Instant

data class OperatingNotice(
    val id: String,
    val shop: RamenShop,
    val type: OperatingNoticeType,
    val description: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val scheduleOverride: BusinessHoursScheduleOverride? = null,
    val manuallyReleasedAt: Instant? = null,
    val sourceUrl: String?,
    val updatedAt: String? = null,
    val dailySchedules: List<OperatingNoticeDay> = emptyList(),
) {
    fun appliesOn(date: LocalDate): Boolean {
        if (date < startDate || (endDate != null && date > endDate)) return false
        if (dailySchedules.isEmpty()) return true
        return dailySchedules.any { it.date == date && it.closed }
    }

    fun temporaryClosureOn(date: LocalDate): BusinessHoursStatus.TemporaryClosure? {
        if (type != OperatingNoticeType.TEMPORARY_CLOSURE || !appliesOn(date)) return null
        if (dailySchedules.isEmpty()) return BusinessHoursStatus.TemporaryClosure(startDate, endDate)
        var firstDate = date
        var lastDate = date
        while (appliesOn(firstDate.minus(1, DateTimeUnit.DAY))) firstDate = firstDate.minus(1, DateTimeUnit.DAY)
        while (appliesOn(lastDate.plus(1, DateTimeUnit.DAY))) lastDate = lastDate.plus(1, DateTimeUnit.DAY)
        return BusinessHoursStatus.TemporaryClosure(firstDate, lastDate)
    }

    fun scheduledDates(referenceDate: LocalDate): List<LocalDate> {
        if (dailySchedules.isNotEmpty()) {
            return dailySchedules
                .filter { it.closed && it.date > referenceDate && appliesOn(it.date) }
                .map { it.date }
                .distinct()
                .sorted()
        }
        val lastDate = endDate ?: startDate
        if (lastDate <= referenceDate) return emptyList()
        return buildList {
            var scheduledDate = maxOf(startDate, referenceDate.plus(1, DateTimeUnit.DAY))
            while (scheduledDate <= lastDate) {
                add(scheduledDate)
                scheduledDate = scheduledDate.plus(1, DateTimeUnit.DAY)
            }
        }
    }

    fun isActiveAt(currentDateTime: LocalDateTime): Boolean = appliesOn(currentDateTime.date)

    fun isCurrentOrScheduledAt(currentDateTime: LocalDateTime): Boolean =
        isActiveAt(currentDateTime) ||
            if (dailySchedules.isEmpty()) startDate > currentDateTime.date else dailySchedules.any { it.closed && it.date > currentDateTime.date }

    fun isRelevantForBusinessHoursAt(currentDateTime: LocalDateTime): Boolean = isCurrentOrScheduledAt(currentDateTime) || businessDateAt(currentDateTime) != null

    fun businessDateAt(currentDateTime: LocalDateTime): LocalDate? {
        if (isActiveAt(currentDateTime)) return currentDateTime.date
        val previousDate = currentDateTime.date.minus(1, DateTimeUnit.DAY)
        if (!appliesOn(previousDate)) return null
        val previousDay = shop.businessHoursDetails?.dayAt(previousDate) ?: return null
        val close = previousDay.close?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        return previousDate.takeIf { previousDay.closeNextDay && currentDateTime.time < close }
    }

    fun nextClosingAt(currentDateTime: LocalDateTime): LocalDateTime? {
        if (!type.hasClosingTime || endTime == null) return null
        return listOf(currentDateTime.date.minus(1, DateTimeUnit.DAY), currentDateTime.date, currentDateTime.date.plus(1, DateTimeUnit.DAY), startDate)
            .filter(::appliesOn)
            .mapNotNull(::closingAt)
            .filter { it > currentDateTime }
            .minOrNull()
    }

    fun closingAt(businessDate: LocalDate): LocalDateTime? {
        val time = endTime ?: return null
        val day = shop.businessHoursDetails?.dayAt(businessDate)
        val open = day?.open?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val date = if (day?.closeNextDay == true && open != null && time < open) businessDate.plus(1, DateTimeUnit.DAY) else businessDate
        return LocalDateTime(date, time)
    }

    fun scheduleOn(
        date: LocalDate,
        hours: BusinessHours?,
    ): BusinessHoursScheduleOverride? {
        if (date < startDate || (endDate != null && date > endDate)) return null
        if (dailySchedules.isNotEmpty()) return dailySchedules.firstOrNull { it.date == date }?.resolve(hours)
        return scheduleOverride
    }
}
