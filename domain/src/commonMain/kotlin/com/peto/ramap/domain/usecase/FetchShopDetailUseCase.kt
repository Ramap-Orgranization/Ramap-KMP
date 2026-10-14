package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult

interface FetchShopDetailUseCase {
    suspend operator fun invoke(shopId: String): RamapResult<ShopDetail>

    fun findCached(shopId: String): ShopDetailCacheLookup

    fun reconcileBookmarkCount(detail: ShopDetail): ShopDetail = (findCached(detail.shop.id) as? ShopDetailCacheLookup.Hit)?.detail?.let { detail.copy(likeCount = it.likeCount) } ?: detail

    fun clearCache()

    fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    )
}
