package com.peto.ramap.network

import kotlinx.serialization.Serializable

@Serializable
internal data class ReverseGeocodeRequest(
    val lat: Double,
    val lng: Double,
)
