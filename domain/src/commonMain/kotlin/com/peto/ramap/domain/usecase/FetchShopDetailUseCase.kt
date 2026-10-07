package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.ShopReviewsPage

interface FetchShopDetailUseCase {
    suspend operator fun invoke(shopId: String): RamapResult<ShopDetail>

    fun findCached(shopId: String): ShopDetailCacheLookup

    fun updateCachedReviews(
        shopId: String,
        page: ShopReviewsPage,
    )

    fun clearCache()

    fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    )
}
