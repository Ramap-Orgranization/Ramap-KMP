package com.peto.ramap.domain.model.shop

data class BusinessHours(
    val weekly: Map<String, BusinessHoursDay>,
    val breakTimes: Map<String, List<BusinessHoursBreakTime>>,
    val lastOrders: Map<String, List<String>>,
    val notice: String?,
    val noticeType: String? = null,
)
