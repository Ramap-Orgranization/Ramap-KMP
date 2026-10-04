package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.businesshour.BusinessDay
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursScheduleOverride
import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
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
    fun isActiveAt(currentDateTime: LocalDateTime): Boolean = currentDateTime.date >= startDate && (endDate == null || currentDateTime.date <= endDate)

    fun isCurrentOrScheduledAt(currentDateTime: LocalDateTime): Boolean = isActiveAt(currentDateTime) || startDate > currentDateTime.date

    fun isRelevantForBusinessHoursAt(currentDateTime: LocalDateTime): Boolean {
        if (isCurrentOrScheduledAt(currentDateTime)) return true
        val previousDate = currentDateTime.date.minus(1, DateTimeUnit.DAY)
        val day = scheduleOn(previousDate, shop.businessHoursDetails)?.day ?: return false
        val overnightDay = if (day.closed) shop.businessHoursDetails?.weekly?.get(BusinessDay.from(previousDate.dayOfWeek).key) ?: return false else day
        val close = overnightDay.close?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return false
        return overnightDay.closeNextDay && currentDateTime.time < close
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
