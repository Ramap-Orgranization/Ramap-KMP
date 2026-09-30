package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.ShopReviewsPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopReviewsPageResponse(
    val reviews: List<ReviewResponse>,
    @SerialName("total_count") val totalCount: Int,
) {
    fun toDomain(): ShopReviewsPage = ShopReviewsPage(reviews.map(ReviewResponse::toDomain), totalCount)
}
