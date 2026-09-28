package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.coroutines.flow.Flow

internal class DefaultReviewRepository(
    private val dataSource: ReviewDataSource,
    private val changes: ReviewChangeNotifier,
) : ReviewRepository {
    override fun observeChanges(): Flow<Unit> = changes.events

    override suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): RamapResult<List<Review>> = invokeRequest { dataSource.fetchShopReviews(shopId, offset).map { it.toDomain() } }

    override suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): RamapResult<List<Review>> = invokeRequest { dataSource.fetchProfileReviews(userId, offset).map { it.toDomain() } }

    override suspend fun submitReview(
        shopId: String,
        body: String,
        images: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            require(Review.isValidBody(body)) { "Invalid review body" }
            require(images.size <= ReviewImage.MAX_COUNT && images.all(ReviewImage::isValid)) { "Invalid review images" }
            dataSource.submitReview(
                ShopReviewRequest(
                    shopId = shopId,
                    body = body.trim(),
                    imagePaths = emptyList(),
                    isPublic = isPublic,
                ),
                images,
            )
            changes.notifyChanged()
        }
}
