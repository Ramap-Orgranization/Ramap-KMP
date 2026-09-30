package com.peto.ramap.ui.main

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ReviewLike
import com.peto.ramap.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeMapReviewRepository : ReviewRepository {
    override fun observeChanges(): Flow<Unit> = emptyFlow()

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage> = RamapResult.Success(MyReviewsPage(emptyList(), 0, 0, 0, true))

    override suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?> = RamapResult.Success(null)

    override suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): RamapResult<List<Review>> = RamapResult.Success(emptyList())

    override suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): RamapResult<List<Review>> = RamapResult.Success(emptyList())

    override suspend fun submitReview(
        shopId: String,
        body: String,
        images: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> = error("Unexpected review submit")

    override suspend fun fetchEditableReview(reviewId: String): RamapResult<EditableReview?> = error("Unexpected editable review fetch")

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> = error("Unexpected review update")

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> = error("Unexpected review delete")

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> = error("Unexpected review like")
}
