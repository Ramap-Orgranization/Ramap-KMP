package com.peto.ramap.domain.model.businesshour

import kotlinx.datetime.LocalDateTime

data class BusinessHours(
    val weekly: Map<String, BusinessHoursDay>,
    val breakTimes: Map<String, List<BreakTime>>,
    val lastOrders: Map<String, List<String>>,
    val notice: String?,
    val noticeType: String? = null,
) {
    fun isOpenAt(currentDateTime: LocalDateTime): Boolean = BusinessHoursStatusCalculator.isOpenAt(this, currentDateTime)

    fun statusAt(currentDateTime: LocalDateTime): BusinessHoursStatus? = BusinessHoursStatusCalculator.statusAt(this, currentDateTime)
}
