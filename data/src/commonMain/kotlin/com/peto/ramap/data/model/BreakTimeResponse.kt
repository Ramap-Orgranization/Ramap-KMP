package com.peto.ramap.data.model

import com.peto.ramap.domain.model.businesshour.BreakTime
import kotlinx.serialization.Serializable

@Serializable
internal data class BreakTimeResponse(
    val start: String,
    val end: String,
) {
    fun toDomain(): BreakTime = BreakTime(start = start, end = end)
}
