package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.ProfileReview
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileReviewResponse(
    val review: ReviewResponse,
    val shop: RamenShopResponse,
) {
    fun toDomain(): ProfileReview = ProfileReview(review = review.toDomain(), shop = shop.toDomain())
}
