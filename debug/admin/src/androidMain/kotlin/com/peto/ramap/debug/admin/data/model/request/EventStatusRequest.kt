package com.peto.ramap.debug.admin.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventStatusRequest(
    val action: String,
    @SerialName("event_id") val eventId: String? = null,
    val status: String? = null,
    val scope: String? = null,
    val reason: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
)
