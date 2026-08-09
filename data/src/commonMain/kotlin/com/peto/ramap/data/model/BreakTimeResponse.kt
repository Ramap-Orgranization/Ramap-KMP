package com.peto.ramap.data.model

import com.peto.ramap.domain.model.shop.BusinessHoursBreakTime
import kotlinx.serialization.Serializable

@Serializable
internal data class BreakTimeResponse(
    val start: String,
    val end: String,
) {
    fun toDomain(): BusinessHoursBreakTime = BusinessHoursBreakTime(start = start, end = end)
}
