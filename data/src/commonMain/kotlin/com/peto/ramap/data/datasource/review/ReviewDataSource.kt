package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.model.EditableReviewResponse
import com.peto.ramap.data.model.ReviewLikeResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.domain.model.review.ReviewImage

internal interface ReviewDataSource {
    suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): List<ReviewResponse>

    suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): List<ReviewResponse>

    suspend fun submitReview(
        review: ShopReviewRequest,
        images: List<ReviewImage>,
    )

    suspend fun fetchEditableReview(reviewId: String): Pair<EditableReviewResponse, List<String?>>?

    suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    )

    suspend fun deleteReview(reviewId: String)

    suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): ReviewLikeResponse
}
