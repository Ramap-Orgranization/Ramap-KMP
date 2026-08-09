package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ImportationRequest(
    val url: String,
)
