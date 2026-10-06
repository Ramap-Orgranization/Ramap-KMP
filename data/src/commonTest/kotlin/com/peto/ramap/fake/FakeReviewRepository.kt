package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.repository.ReviewRepository

class FakeReviewRepository(
    var reviews: List<Review> = emptyList(),
    var error: RamapError? = null,
) : ReviewRepository {
    val requestedShopReviews = mutableListOf<Pair<String, Long>>()

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage> = RamapResult.Success(MyReviewsPage(emptyList(), 0, 0, 0, true))

    override suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?> = RamapResult.Success(null)

    override suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): RamapResult<ShopReviewsPage> {
        requestedShopReviews += shopId to offset
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(ShopReviewsPage(reviews, reviews.size))
    }

    override suspend fun fetchBlockedShopReviewOnce(reviewId: String): RamapResult<Review?> = error("Unexpected blocked review fetch")

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
