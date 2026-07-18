package com.peto.ramap.data.model

import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.shop.Location
import kotlinx.serialization.Serializable

@Serializable
data class PlaceSearchResultResponse(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
) {
    fun toDomain(): PlaceSearchResult =
        PlaceSearchResult(
            name = name,
            address = address,
            location = Location(lat = lat, lng = lng),
        )
}
