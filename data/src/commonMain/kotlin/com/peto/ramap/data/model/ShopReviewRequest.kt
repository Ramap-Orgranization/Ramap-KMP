package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopReviewRequest(
    @SerialName("shop_id")
    val shopId: String,
    val body: String,
    @SerialName("image_paths")
    val imagePaths: List<String>,
    @SerialName("is_public")
    val isPublic: Boolean = true,
)
