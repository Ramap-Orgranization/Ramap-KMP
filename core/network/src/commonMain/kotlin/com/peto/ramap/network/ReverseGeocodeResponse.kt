package com.peto.ramap.network

import kotlinx.serialization.Serializable

@Serializable
internal data class ReverseGeocodeResponse(
    val address: String?,
)
