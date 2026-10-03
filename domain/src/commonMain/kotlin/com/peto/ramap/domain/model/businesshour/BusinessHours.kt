package com.peto.ramap.domain.model.businesshour

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class BusinessHours(
    val weekly: Map<String, BusinessHoursDay>,
    val breakTimes: Map<String, List<BreakTime>>,
    val lastOrders: Map<String, List<String>>,
    val notice: String?,
    val noticeType: String? = null,
    val dateOverrides: Map<LocalDate, BusinessHoursScheduleOverride> = emptyMap(),
) {
    fun dayAt(date: LocalDate): BusinessHoursDay? = dateOverrides[date]?.day ?: weekly[BusinessDay.from(date.dayOfWeek).key]

    fun breaksAt(date: LocalDate): List<BreakTime> = dateOverrides[date]?.breakTimes ?: breakTimes[BusinessDay.from(date.dayOfWeek).key].orEmpty()

    fun lastOrdersAt(date: LocalDate): List<String> = if (dateOverrides[date]?.useRegularLastOrders == false) emptyList() else lastOrders[BusinessDay.from(date.dayOfWeek).key].orEmpty()

    fun statusAt(currentDateTime: LocalDateTime): BusinessHoursStatus? = BusinessHoursStatusCalculator.statusAt(this, currentDateTime)
}
