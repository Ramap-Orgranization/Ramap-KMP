package com.peto.ramap.network.client.importation

import kotlinx.serialization.Serializable

@Serializable
data class ImportationResponse(
    val provider: String,
    val totalPlaceCount: Int,
    val matchedShopIds: List<String>,
    val unmatchedPlaceNames: List<String>,
)
