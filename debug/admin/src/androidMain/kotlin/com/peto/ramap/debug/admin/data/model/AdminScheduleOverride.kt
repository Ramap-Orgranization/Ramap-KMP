package com.peto.ramap.debug.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminScheduleOverride(
    val closed: Boolean = false,
    val open: String? = null,
    val close: String? = null,
    @SerialName("close_next_day") val closeNextDay: Boolean = false,
    val label: String? = null,
    @SerialName("break_times") val breakTimes: List<AdminBreakTime> = emptyList(),
)

@Serializable
internal data class AdminBreakTime(
    val start: String,
    val end: String,
)
