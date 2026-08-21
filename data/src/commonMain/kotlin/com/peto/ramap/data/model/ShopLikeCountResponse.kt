package com.peto.ramap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopLikeCountResponse(
    @SerialName("shop_id") val shopId: String,
    @SerialName("like_count") val likeCount: Long,
)
