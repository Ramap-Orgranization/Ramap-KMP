package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.ShopReviewsPage
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeChanges(): Flow<Unit>

    suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage>

    suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?>

    suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): RamapResult<List<Review>>

    suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): RamapResult<ShopReviewsPage> =
        when (val result = fetchShopReviews(shopId, offset)) {
            is RamapResult.Error -> result
            is RamapResult.Success -> RamapResult.Success(ShopReviewsPage(result.data, result.data.size))
        }

    suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): RamapResult<List<Review>>

    suspend fun submitReview(
        shopId: String,
        body: String,
        images: List<ReviewImage>,
        isPublic: Boolean = true,
    ): RamapResult<Unit>

    suspend fun fetchEditableReview(reviewId: String): RamapResult<EditableReview?>

    suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit>

    suspend fun deleteReview(reviewId: String): RamapResult<Unit>

    suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike>
}
