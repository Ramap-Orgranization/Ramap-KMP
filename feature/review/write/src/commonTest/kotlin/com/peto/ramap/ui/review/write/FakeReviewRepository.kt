package com.peto.ramap.ui.review.write

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal class FakeReviewRepository(
    private val submitResult: RamapResult<Unit> = RamapResult.Success(Unit),
    private val submitPending: CompletableDeferred<RamapResult<Unit>>? = null,
    private val ignoreSubmitCancellation: Boolean = false,
) : ReviewRepository {
    val submissions = mutableListOf<Submission>()
    var editableReview: EditableReview? = null
    var editResult: RamapResult<Unit> = RamapResult.Success(Unit)
    val updates = mutableListOf<Update>()

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage> = RamapResult.Success(MyReviewsPage(emptyList(), 0, 0, 0, true))

    override suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?> = RamapResult.Success(null)

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
    ): RamapResult<Unit> {
        submissions += Submission(shopId, body, images, isPublic)
        val pending = submitPending ?: return submitResult
        return if (ignoreSubmitCancellation) withContext(NonCancellable) { pending.await() } else pending.await()
    }

    data class Submission(
        val shopId: String,
        val body: String,
        val images: List<ReviewImage>,
        val isPublic: Boolean,
    )

    data class Update(
        val reviewId: String,
        val body: String,
        val retainedImagePaths: List<String>,
        val newImages: List<ReviewImage>,
        val isPublic: Boolean,
    )

    override suspend fun fetchEditableReview(reviewId: String): RamapResult<EditableReview?> = RamapResult.Success(editableReview)

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> {
        updates += Update(reviewId, body, retainedImagePaths, newImages, isPublic)
        return editResult
    }

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> = error("Unexpected review deletion")

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> = error("Unexpected review like")
}
