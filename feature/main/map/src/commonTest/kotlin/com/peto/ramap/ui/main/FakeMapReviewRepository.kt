package com.peto.ramap.ui.main

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

class FakeMapReviewRepository : ReviewRepository {
    var blockedReview: Review? = null
    val requestedBlockedReviews = mutableListOf<String>()

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage> = RamapResult.Success(MyReviewsPage(emptyList(), 0, 0, 0, true))

    override suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?> = RamapResult.Success(null)

    override suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): RamapResult<ShopReviewsPage> = RamapResult.Success(ShopReviewsPage(emptyList(), 0))

    override suspend fun fetchBlockedShopReviewOnce(reviewId: String): RamapResult<Review?> {
        requestedBlockedReviews += reviewId
        return RamapResult.Success(blockedReview)
    }

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
