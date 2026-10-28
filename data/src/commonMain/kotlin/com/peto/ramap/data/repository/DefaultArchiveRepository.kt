package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ArchiveRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultArchiveRepository(
    private val reviews: ReviewDataSource,
    private val shops: RamenShopDataSource,
    private val images: ImageUrlDataSource,
) : ArchiveRepository {
    override suspend fun fetchArchiveShops(): RamapResult<ArchiveShops> =
        invokeRequest {
            val rows = mutableListOf<MyReview>()
            var offset = 0L
            do {
                val page = reviews.fetchMyReviewsMetadata(offset)
                rows += page.reviews.map { it.toDomain() }
                // Offset counts server rows, not deduplicated IDs.
                offset += page.reviews.size
            } while (page.reviews.isNotEmpty() && offset < page.totalCount)
            val unique = rows.distinctBy { it.review.id }
            ArchiveShops(unique, fetchShopRows(unique.map { it.review.shopId }.toSet()))
        }

    override suspend fun fetchShops(shopIds: Set<String>): RamapResult<RamenShops> = invokeRequest { fetchShopRows(shopIds) }

    private suspend fun fetchShopRows(shopIds: Set<String>): RamenShops {
        val result = shopIds.chunked(100).flatMap { shops.fetchRamenShopsByIds(it.toSet()) }
        return RamenShops(result.map { it.toDomain() })
    }

    override suspend fun resolvePhotos(references: Set<String>): RamapResult<Map<String, MyReviewPhoto>> =
        invokeRequest {
            val requests = references.map { SignedImageRequest("shop-review-images", it) }
            val signed = images.fetchUrls(requests)
            requests.associate { request ->
                val image = signed[request]
                request.path to MyReviewPhoto(image?.url, request.path, image?.expiresAt)
            }
        }
}
