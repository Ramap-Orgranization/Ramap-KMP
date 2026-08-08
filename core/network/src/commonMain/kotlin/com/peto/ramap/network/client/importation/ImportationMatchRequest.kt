package com.peto.ramap.network.client.importation

import kotlinx.serialization.Serializable

@Serializable
data class ImportationMatchRequest(
    val provider: String,
    val places: List<ImportationPlaceRequest>,
)
