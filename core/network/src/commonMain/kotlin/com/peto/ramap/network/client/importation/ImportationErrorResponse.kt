package com.peto.ramap.network.client.importation

import kotlinx.serialization.Serializable

@Serializable
data class ImportationErrorResponse(
    val code: String,
)
