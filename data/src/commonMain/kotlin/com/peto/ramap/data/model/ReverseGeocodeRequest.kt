package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ReverseGeocodeRequest(
    val lat: Double,
    val lng: Double,
)
