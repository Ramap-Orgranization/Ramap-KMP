package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.ShopOverView

interface FetchShopOverviewUseCase {
    suspend operator fun invoke(shopId: String): RamapResult<ShopOverView>

    fun findCached(shopId: String): ShopOverviewCacheLookup

    fun reconcileBookmarkCount(overview: ShopOverView): ShopOverView = (findCached(overview.shop.id) as? ShopOverviewCacheLookup.Hit)?.overview?.let { overview.copy(likeCount = it.likeCount) } ?: overview

    fun clearCache()

    fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    )
}
