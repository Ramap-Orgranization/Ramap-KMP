package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ReverseGeocodeResponse(
    val address: String?,
)
