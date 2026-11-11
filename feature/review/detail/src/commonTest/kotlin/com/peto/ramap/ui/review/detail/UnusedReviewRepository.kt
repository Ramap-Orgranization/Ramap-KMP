package com.peto.ramap.ui.review.detail

import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ReviewRepository

internal object UnusedReviewRepository : ReviewRepository {
    override suspend fun fetchMyReviewCount(): Nothing = unexpected()

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): Nothing = unexpected()

    override suspend fun fetchMyReview(reviewId: String): Nothing = unexpected()

    override suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): Nothing = unexpected()

    override suspend fun fetchBlockedShopReviewOnce(reviewId: String): Nothing = unexpected()

    override suspend fun submitReview(
        shopId: String,
        body: String,
        images: List<ReviewImage>,
        isPublic: Boolean,
    ): Nothing = unexpected()

    override suspend fun fetchEditableReview(reviewId: String): Nothing = unexpected()

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): Nothing = unexpected()

    override suspend fun deleteReview(reviewId: String): Nothing = unexpected()

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): Nothing = unexpected()

    private fun unexpected(): Nothing = error("댓글 테스트에서 리뷰 변경을 호출했습니다")
}
