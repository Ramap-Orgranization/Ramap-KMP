package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ReviewLike
import com.peto.ramap.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeReviewRepository(
    var reviews: List<Review> = emptyList(),
    var error: RamapError? = null,
) : ReviewRepository {
    val requestedShopReviews = mutableListOf<Pair<String, Long>>()

    override fun observeChanges(): Flow<Unit> = emptyFlow()

    override suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): RamapResult<List<Review>> {
        requestedShopReviews += shopId to offset
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(reviews)
    }

    override suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): RamapResult<List<Review>> = RamapResult.Success(emptyList())

    override suspend fun submitReview(
        shopId: String,
        body: String,
        images: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun fetchEditableReview(reviewId: String): RamapResult<EditableReview?> = error("Unexpected editable review fetch")

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> = error("Unexpected review update")

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> = error("Unexpected review deletion")

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> = error("Unexpected review like")
}
