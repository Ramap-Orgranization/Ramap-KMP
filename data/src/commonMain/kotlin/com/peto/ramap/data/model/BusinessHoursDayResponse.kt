package com.peto.ramap.data.model

import com.peto.ramap.domain.model.shop.BusinessHoursDay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BusinessHoursDayResponse(
    val closed: Boolean = false,
    val open: String? = null,
    val close: String? = null,
    @SerialName("close_next_day") val closeNextDay: Boolean = false,
    val label: String? = null,
) {
    fun toDomain(): BusinessHoursDay =
        BusinessHoursDay(
            closed = closed,
            open = open,
            close = close,
            closeNextDay = closeNextDay,
            label = label,
        )
}
