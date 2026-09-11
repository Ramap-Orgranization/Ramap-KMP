package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.businesshour.BusinessHoursScheduleOverride
import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
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
) {
    fun isActiveAt(currentDateTime: LocalDateTime): Boolean = currentDateTime.date >= startDate && (endDate == null || currentDateTime.date <= endDate)
}
