package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminShopHours(
    @SerialName("business_hours_weekly") val weekly: Map<String, AdminScheduleOverride> = emptyMap(),
    @SerialName("business_hours_break_times") val breakTimes: Map<String, List<AdminBreakTime>> = emptyMap(),
)
