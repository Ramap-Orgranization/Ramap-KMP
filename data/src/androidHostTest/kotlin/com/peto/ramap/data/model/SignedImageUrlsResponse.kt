package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SignedImageUrlsResponse(
    val images: List<SignedImageResponse>,
)
