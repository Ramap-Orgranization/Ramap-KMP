package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StorageSignedImageResponse(
    val path: String,
    @SerialName("signedURL") val signedUrl: String? = null,
    val error: String? = null,
)
