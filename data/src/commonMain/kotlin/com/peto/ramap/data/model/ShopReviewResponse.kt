package com.peto.ramap.data.model

import com.peto.ramap.domain.model.shop.ShopReview
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopReviewResponse(
    val id: String,
    @SerialName("shop_id")
    val shopId: String,
    val body: String,
    @SerialName("created_at")
    val createdAt: String,
) {
    fun toDomain(): ShopReview =
        ShopReview(
            id = id,
            shopId = shopId,
            body = body,
            createdAt = createdAt,
        )
}
