package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.model.ReviewMutationResponse
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.data.store.DefaultReviewChangeStore
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultReviewRepository(
    private val dataSource: ReviewDataSource,
    private val shopDataSource: RamenShopDataSource,
    private val changes: DefaultReviewChangeStore,
) : ReviewRepository {
    private val writes = Mutex()
    private val hydrationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: MyReviewVisibility,
    ): RamapResult<MyReviewsPage> =
        invokeRequest {
            val page = dataSource.fetchMyReviews(offset, visibility.rpcValue).toDomain()
            val shops = fetchReviewShops(page.reviews)
            page.copy(reviews = page.reviews.map { it.copy(shop = shops[it.shopId]) })
        }

    private suspend fun fetchReviewShops(reviews: List<MyReview>): Map<String, RamenShop> {
        val shopIds = reviews.map { it.shopId }.filter { it.isNotBlank() }.toSet()
        if (shopIds.isEmpty()) return emptyMap()
        val result =
            invokeRequest {
                shopDataSource.fetchRamenShopsByIds(shopIds).map { it.toDomain() }.associateBy { it.id }
            }
        return when (result) {
            is RamapResult.Success -> result.data
            is RamapResult.Error -> emptyMap()
        }
    }

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
        write {
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
        write {
            require(Review.isValidBody(body)) { ERROR_INVALID_REVIEW_BODY }
            require(retainedImagePaths.size + newImages.size <= ReviewImage.MAX_COUNT) { ERROR_TOO_MANY_IMAGES }
            require(newImages.all(ReviewImage::isValid)) { ERROR_INVALID_REVIEW_IMAGES }
            dataSource.updateReview(reviewId, body.trim(), retainedImagePaths, newImages, isPublic)
        }

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> = write { dataSource.deleteReview(reviewId) }

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): RamapResult<ReviewLike> =
        invokeRequest {
            val epoch = changes.synchronizeSession()
            dataSource.setReviewLike(reviewId, liked).toDomain().also { changes.publish(ReviewChange.Liked(reviewId, it), epoch) }
        }

    private suspend fun write(request: suspend () -> ReviewMutationResponse): RamapResult<Unit> =
        writes.withLock {
            invokeRequest {
                val epoch = changes.synchronizeSession()
                val receipt = request()
                changes.publish(receipt.toDomain(), epoch)
                hydrate(receipt, epoch)
            }
        }

    private suspend fun hydrate(
        receipt: ReviewMutationResponse,
        epoch: Long,
    ) {
        val completed = hydrateOnce(receipt, epoch)
        if (completed || receipt.after == null) return
        hydrationScope.launch {
            repeat(HYDRATION_RETRIES) { attempt ->
                delay(HYDRATION_RETRY_DELAY_MS * (attempt + 1))
                if (changes.synchronizeSession() != epoch || hydrateOnce(receipt, epoch)) return@launch
            }
        }
    }

    private suspend fun hydrateOnce(
        receipt: ReviewMutationResponse,
        epoch: Long,
    ): Boolean =
        try {
            val hydrated = dataSource.hydrateMutation(receipt).toDomain().after
            if (hydrated != null) changes.publish(ReviewChange.Hydrated(hydrated), epoch)
            hydrated?.review?.images?.all { it.url != null } != false
        } catch (_: CancellationException) {
            false
        } catch (_: Exception) {
            false
        }

    private companion object {
        const val HYDRATION_RETRIES = 3
        const val HYDRATION_RETRY_DELAY_MS = 1000L
        const val ERROR_INVALID_REVIEW_BODY = "Invalid review body"
        const val ERROR_INVALID_REVIEW_IMAGES = "Invalid review images"
        const val ERROR_TOO_MANY_IMAGES = "Too many images"
    }
}
