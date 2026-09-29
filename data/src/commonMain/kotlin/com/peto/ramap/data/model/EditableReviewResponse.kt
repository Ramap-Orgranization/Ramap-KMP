package com.peto.ramap.data.model

import com.peto.ramap.domain.model.review.EditableReview
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EditableReviewResponse(
    @SerialName("review_id")
    val reviewId: String,
    @SerialName("shop_id")
    val shopId: String,
    val body: String,
    @SerialName("image_paths")
    val imagePaths: List<String>,
    @SerialName("is_public")
    val isPublic: Boolean,
) {
    fun toDomain(imageUrls: List<String?>): EditableReview = EditableReview(reviewId, shopId, body, imagePaths, imageUrls, isPublic)
}
