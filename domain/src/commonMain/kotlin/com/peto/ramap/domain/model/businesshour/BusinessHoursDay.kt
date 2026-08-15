package com.peto.ramap.domain.model.businesshour

data class BusinessHoursDay(
    val closed: Boolean,
    val open: String?,
    val close: String?,
    val closeNextDay: Boolean,
    val label: String?,
)
