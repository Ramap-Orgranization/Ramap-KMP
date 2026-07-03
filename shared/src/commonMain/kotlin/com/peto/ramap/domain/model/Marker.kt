package com.peto.ramap.domain.model

sealed interface Marker {
    val id: String
    val location: Location

    data class SingleMarker(
        val shop: RamenShop,
    ) : Marker {
        override val id: String = shop.id
        override val location: Location = shop.location
    }

    data class ClusterMaker(
        override val id: String,
        override val location: Location,
        val shops: List<RamenShop>,
    ) : Marker {
        val count: Int
            get() = shops.size
    }
}
