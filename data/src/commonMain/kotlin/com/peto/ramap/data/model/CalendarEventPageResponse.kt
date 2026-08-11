package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CalendarEventPageResponse(
    val events: List<ShopEventResponse> = emptyList(),
    @SerialName("has_previous") val hasPrevious: Boolean = false,
    @SerialName("has_next") val hasNext: Boolean = false,
)
