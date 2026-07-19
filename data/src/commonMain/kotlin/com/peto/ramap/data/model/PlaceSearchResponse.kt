package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaceSearchResponse(
    val results: List<PlaceSearchResultResponse>,
)
