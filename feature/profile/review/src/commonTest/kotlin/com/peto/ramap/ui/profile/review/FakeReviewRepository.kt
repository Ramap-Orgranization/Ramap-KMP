package com.peto.ramap.ui.profile.review

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ReviewLike
import com.peto.ramap.domain.repository.ReviewRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

internal class FakeReviewRepository(
    private val reviews: List<Review> = emptyList(),
    private val profileReviews: List<Review> = emptyList(),
) : ReviewRepository {
    var nextProfileResponse: CompletableDeferred<RamapResult<List<Review>>>? = null
    var error: RamapError? = null
    val profileRequests = mutableListOf<Pair<String?, Long>>()
    val likedRequests = mutableListOf<Pair<String, Boolean>>()
    val deletedReviewIds = mutableListOf<String>()
    var mutationError: RamapError? = null

    override fun observeChanges(): Flow<Unit> = emptyFlow()

    override suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): RamapResult<List<Review>> = error?.let { RamapResult.Error(it) } ?: RamapResult.Success(reviews.drop(offset.toInt()).take(20))

    override suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): RamapResult<List<Review>> {
        profileRequests += userId to offset
        val pending = nextProfileResponse
        nextProfileResponse = null
        if (pending != null) return withContext(NonCancellable) { pending.await() }
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(profileReviews.drop(offset.toInt()).take(20))
    }

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

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> {
        deletedReviewIds += reviewId
        return mutationError?.let { RamapResult.Error(it) } ?: RamapResult.Success(Unit)
    }

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> {
        likedRequests += reviewId to liked
        return mutationError?.let { RamapResult.Error(it) } ?: RamapResult.Success(ReviewLike(1, liked))
    }
}
