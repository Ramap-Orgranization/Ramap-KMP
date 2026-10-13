package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaceSearchErrorResponse(
    val code: String? = null,
    @SerialName("limit_window") val limitWindow: String? = null,
    @SerialName("retry_after_seconds") val retryAfterSeconds: Long? = null,
)
