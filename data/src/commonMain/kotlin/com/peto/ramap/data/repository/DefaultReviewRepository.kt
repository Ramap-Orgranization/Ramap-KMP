package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.coroutines.flow.Flow

internal class DefaultReviewRepository(
    private val dataSource: ReviewDataSource,
    private val changes: ReviewChangeNotifier,
) : ReviewRepository {
    override fun observeChanges(): Flow<Unit> = changes.events

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage> = invokeRequest { dataSource.fetchMyReviews(offset, visibility.rpcValue).toDomain() }

    override suspend fun fetchMyReview(reviewId: String): RamapResult<MyReview?> = invokeRequest { dataSource.fetchMyReview(reviewId)?.toDomain() }

    override suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): RamapResult<ShopReviewsPage> = invokeRequest { dataSource.fetchShopReviewsPage(shopId, offset).toDomain() }

    override suspend fun fetchBlockedShopReviewOnce(
        reviewId: String,
    ): RamapResult<Review?> = invokeRequest { dataSource.fetchBlockedShopReviewOnce(reviewId)?.toDomain() }

    override suspend fun submitReview(
        shopId: String,
        body: String,
        images: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            require(Review.isValidBody(body)) { ERROR_INVALID_REVIEW_BODY }
            require(images.size <= ReviewImage.MAX_COUNT && images.all(ReviewImage::isValid)) { ERROR_INVALID_REVIEW_IMAGES }
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

    override suspend fun fetchEditableReview(reviewId: String): RamapResult<EditableReview?> =
        invokeRequest {
            dataSource.fetchEditableReview(reviewId)?.let { (response, urls) -> response.toDomain(urls) }
        }

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            require(Review.isValidBody(body)) { ERROR_INVALID_REVIEW_BODY }
            require(retainedImagePaths.size + newImages.size <= ReviewImage.MAX_COUNT) { ERROR_TOO_MANY_IMAGES }
            require(newImages.all(ReviewImage::isValid)) { ERROR_INVALID_REVIEW_IMAGES }
            dataSource.updateReview(reviewId, body.trim(), retainedImagePaths, newImages, isPublic)
            changes.notifyChanged()
        }

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> =
        invokeRequest {
            dataSource.deleteReview(reviewId)
            changes.notifyChanged()
        }

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> =
        invokeRequest {
            dataSource.setReviewLike(reviewId, liked).toDomain()
        }

    private companion object {
        const val ERROR_INVALID_REVIEW_BODY = "Invalid review body"
        const val ERROR_INVALID_REVIEW_IMAGES = "Invalid review images"
        const val ERROR_TOO_MANY_IMAGES = "Too many images"
    }
}
