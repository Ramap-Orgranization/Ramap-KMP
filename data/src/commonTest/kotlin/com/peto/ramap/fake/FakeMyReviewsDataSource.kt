package com.peto.ramap.fake

import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.model.EditableReviewResponse
import com.peto.ramap.data.model.MyReviewResponse
import com.peto.ramap.data.model.MyReviewsPageResponse
import com.peto.ramap.data.model.ReviewLikeResponse
import com.peto.ramap.data.model.ReviewMutationResponse
import com.peto.ramap.data.model.ReviewOwnerCountsResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.domain.model.review.ReviewImage

internal class FakeMyReviewsDataSource(
    private val myReviewsPage: MyReviewsPageResponse? = null,
    private val myReview: MyReviewResponse? = null,
) : ReviewDataSource {
    var submittedReview: ShopReviewRequest? = null
    var hydrationFailure: Exception? = null

    override suspend fun hydrateMutation(response: ReviewMutationResponse): ReviewMutationResponse {
        hydrationFailure?.let { throw it }
        return response
    }

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: String,
    ): MyReviewsPageResponse = myReviewsPage ?: error("Unexpected my reviews fetch")

    override suspend fun fetchMyReview(reviewId: String): MyReviewResponse? = myReview?.takeIf { it.id == reviewId }

    override suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): List<ReviewResponse> = emptyList()

    override suspend fun fetchBlockedShopReviewOnce(reviewId: String): ReviewResponse? = error("Unexpected blocked review fetch")

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): com.peto.ramap.data.model.ProfileReviewsPageResponse = error("Unexpected profile page")

    override suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): List<ReviewResponse> = emptyList()

    override suspend fun submitReview(
        review: ShopReviewRequest,
        images: List<ReviewImage>,
    ): ReviewMutationResponse {
        submittedReview = review
        return ReviewMutationResponse("review", "owner", review.shopId, after = ReviewResponse("review", review.shopId, review.body, "2026-10-12", userId = "owner", isPublic = review.isPublic), counts = ReviewOwnerCountsResponse(1, if (review.isPublic) 1 else 0, if (review.isPublic) 0 else 1, 0, true))
    }

    override suspend fun fetchEditableReview(reviewId: String): Pair<EditableReviewResponse, List<String?>>? = error("Unexpected editable review fetch")

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): ReviewMutationResponse {
        error("Unexpected review update")
    }

    override suspend fun deleteReview(reviewId: String): ReviewMutationResponse {
        error("Unexpected review deletion")
    }

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): ReviewLikeResponse = error("Unexpected review like")
}
