package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAttachment
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.repository.ReviewRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal class FakeMyTabReviewRepository(
    var totalCount: Int = 0,
) : ReviewRepository {
    var countFetchCount = 0
    var countPending: CompletableDeferred<RamapResult<Int>>? = null
    var ignoreCountCancellation = false

    override suspend fun fetchMyReviewCount(): RamapResult<Int> {
        countFetchCount++
        val pending = countPending ?: return RamapResult.Success(totalCount)
        return if (ignoreCountCancellation) withContext(NonCancellable) { pending.await() } else pending.await()
    }

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage> = error("마이탭은 리뷰 목록을 조회하면 안 됩니다")

    override suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): RamapResult<ShopReviewsPage> = RamapResult.Success(ShopReviewsPage(emptyList(), 0))

    override suspend fun fetchBlockedShopReviewOnce(reviewId: String): RamapResult<Review?> = error("Unexpected blocked review fetch")

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
        attachments: List<ReviewAttachment>,
        isPublic: Boolean,
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> = RamapResult.Success(ReviewLike(0, liked))
}
