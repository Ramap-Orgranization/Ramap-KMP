package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeChanges(): Flow<Unit>

    suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage>

    suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?>

    suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): RamapResult<ShopReviewsPage>

    suspend fun fetchBlockedShopReviewOnce(reviewId: String): RamapResult<Review?>

    suspend fun submitReview(
        shopId: String,
        body: String,
        images: List<ReviewImage>,
        isPublic: Boolean = DEFAULT_IS_PUBLIC,
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

    companion object {
        const val DEFAULT_IS_PUBLIC: Boolean = true
    }
}
