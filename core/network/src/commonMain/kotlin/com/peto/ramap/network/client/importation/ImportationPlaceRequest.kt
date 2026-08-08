package com.peto.ramap.network.client.importation

import kotlinx.serialization.Serializable

@Serializable
data class ImportationPlaceRequest(
    val sourceId: String? = null,
    val name: String,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)
