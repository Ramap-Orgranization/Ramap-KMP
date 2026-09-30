package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ReviewLike
import com.peto.ramap.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableSharedFlow

internal class FakeMyTabReviewRepository(
    var totalCount: Int = 0,
) : ReviewRepository {
    val changes = MutableSharedFlow<Unit>()

    override fun observeChanges() = changes

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ) = RamapResult.Success(MyReviewsPage(emptyList(), totalCount, totalCount, 0, false))

    override suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?> {
        TODO("Not yet implemented")
    }

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
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun fetchEditableReview(reviewId: String): RamapResult<EditableReview?> = RamapResult.Success(null)

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> = RamapResult.Success(ReviewLike(0, liked))
}
