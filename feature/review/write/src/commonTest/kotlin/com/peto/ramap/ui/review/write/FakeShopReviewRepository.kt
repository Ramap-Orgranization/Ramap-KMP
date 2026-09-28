package com.peto.ramap.ui.review.write

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ShopReviewRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

internal class FakeShopReviewRepository(
    private val submitResult: RamapResult<Unit> = RamapResult.Success(Unit),
    private val submitPending: CompletableDeferred<RamapResult<Unit>>? = null,
    private val ignoreSubmitCancellation: Boolean = false,
) : ShopReviewRepository {
    val submissions = mutableListOf<Submission>()

    override fun observeChanges(): Flow<Unit> = emptyFlow()

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
}
