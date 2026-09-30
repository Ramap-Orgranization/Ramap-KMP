package com.peto.ramap.ui.review.my

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.repository.ReviewRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

internal class FakeOwnerReviews(
    private val total: Int = 12,
) : ReviewRepository {
    val calls = mutableListOf<Pair<Long, MyReviewVisibility>>()
    val changed = MutableSharedFlow<Unit>()
    var failNext = false
    var deleteResult: RamapResult<Unit> = RamapResult.Success(Unit)
    val pendingResults = mutableListOf<CompletableDeferred<RamapResult<MyReviewsPage>>>()
    var deleteCalls = 0

    override fun observeChanges() = changed

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage> {
        calls += offset to visibility
        if (pendingResults.isNotEmpty()) return withContext(NonCancellable) { pendingResults.removeAt(0).await() }
        if (failNext) {
            failNext = false
            return RamapResult.Error(RamapError.Network(IllegalStateException("offline")))
        }
        val all = (0 until total).map { review(it) }
        val filtered =
            when (visibility) {
                MyReviewVisibility.ALL -> all
                MyReviewVisibility.PUBLIC -> all.filter { it.isPublic }
                MyReviewVisibility.PRIVATE -> all.filterNot { it.isPublic }
            }
        return RamapResult.Success(MyReviewsPage(filtered.drop(offset.toInt()).take(30), total, total - 2, 2, false))
    }

    override suspend fun fetchMyReview(reviewId: String) = RamapResult.Success(review(reviewId.toInt()))

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> {
        deleteCalls++
        return deleteResult
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
    ) = RamapResult.Success(Unit)

    override suspend fun fetchEditableReview(reviewId: String): RamapResult<EditableReview?> = RamapResult.Success(null)

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ) = RamapResult.Success(Unit)

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> = RamapResult.Success(ReviewLike(0, liked))

    private fun review(index: Int) =
        MyReview(
            id = "$index",
            shopId = "shop",
            shopName = "매장",
            body = "리뷰 본문",
            createdAt = "2026-09-28",
            images = emptyList(),
            isPublic = index < total - 2,
            moderationStatus = ReviewModerationStatus.PUBLISHED,
        )
}
