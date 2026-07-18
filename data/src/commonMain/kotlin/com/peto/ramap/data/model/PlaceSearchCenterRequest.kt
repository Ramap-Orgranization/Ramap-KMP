package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaceSearchCenterRequest(
    val lat: Double,
    val lng: Double,
)
