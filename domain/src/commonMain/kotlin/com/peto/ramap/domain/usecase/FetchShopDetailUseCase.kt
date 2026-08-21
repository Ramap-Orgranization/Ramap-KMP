package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult

interface FetchShopDetailUseCase {
    suspend operator fun invoke(shopId: String): RamapResult<ShopDetail>

    fun findCached(shopId: String): ShopDetailCacheLookup

    fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    )
}
