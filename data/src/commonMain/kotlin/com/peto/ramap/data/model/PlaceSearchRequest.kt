package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaceSearchRequest(
    val query: String,
    val center: PlaceSearchCenterRequest,
)
