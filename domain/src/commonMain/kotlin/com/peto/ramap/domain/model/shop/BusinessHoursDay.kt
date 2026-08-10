package com.peto.ramap.domain.model.shop

data class BusinessHoursDay(
    val closed: Boolean,
    val open: String?,
    val close: String?,
    val closeNextDay: Boolean,
    val label: String?,
)
