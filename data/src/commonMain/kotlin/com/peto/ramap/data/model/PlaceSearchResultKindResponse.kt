package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class PlaceSearchResultKindResponse {
    @SerialName("map_location")
    MAP_LOCATION,

    @SerialName("registered_shop")
    REGISTERED_SHOP,
}
