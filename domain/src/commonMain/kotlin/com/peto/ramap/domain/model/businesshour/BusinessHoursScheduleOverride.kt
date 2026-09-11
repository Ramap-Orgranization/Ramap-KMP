package com.peto.ramap.domain.model.businesshour

data class BusinessHoursScheduleOverride(
    val day: BusinessHoursDay,
    val breakTimes: List<BreakTime> = emptyList(),
)
