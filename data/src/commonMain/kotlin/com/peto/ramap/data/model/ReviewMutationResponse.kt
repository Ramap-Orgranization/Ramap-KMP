package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewMutationResponse(
    @SerialName("review_id") val reviewId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("shop_id") val shopId: String,
    val before: ReviewResponse? = null,
    val after: ReviewResponse? = null,
    @SerialName("owner_counts") val counts: ReviewOwnerCountsResponse,
) {
    fun toDomain(): ReviewChange.Written =
        ReviewChange.Written(
            reviewId,
            userId,
            shopId,
            before?.let { ReviewSnapshot(it.toDomain(), it.shopName, it.moderationNote) },
            after?.let { ReviewSnapshot(it.toDomain(), it.shopName, it.moderationNote) },
            counts.toDomain(),
        )
}
