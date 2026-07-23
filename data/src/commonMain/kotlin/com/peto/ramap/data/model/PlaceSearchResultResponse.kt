package com.peto.ramap.data.model

import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.place.PlaceSearchResultKind
import com.peto.ramap.domain.model.shop.Location
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PlaceSearchResultResponse(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val kind: PlaceSearchResultKindResponse? = null,
    @SerialName("shop_id")
    val shopId: String? = null,
) {
    fun toDomain(): PlaceSearchResult =
        PlaceSearchResult(
            name = name,
            address = address,
            location = Location(lat = lat, lng = lng),
            kind = kind.toDomain(),
            shopId = shopId,
        )

    private fun PlaceSearchResultKindResponse?.toDomain(): PlaceSearchResultKind =
        when (this) {
            PlaceSearchResultKindResponse.MAP_LOCATION -> PlaceSearchResultKind.MAP_LOCATION
            PlaceSearchResultKindResponse.REGISTERED_SHOP -> PlaceSearchResultKind.REGISTERED_SHOP
            null -> PlaceSearchResultKind.UNCLASSIFIED
        }
}
