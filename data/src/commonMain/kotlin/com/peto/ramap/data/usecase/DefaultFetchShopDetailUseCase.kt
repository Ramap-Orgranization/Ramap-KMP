package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup

internal class DefaultFetchShopDetailUseCase(
    private val ramenShopRepository: RamenShopRepository,
) : FetchShopDetailUseCase {
    private val cache = mutableMapOf<String, ShopDetail>()

    /**
     * 매장 상세를 조회한다.
     *
     * 캐시된 매장·좋아요·웨이팅은 유지하고 서버의 최신 이벤트·공지·메뉴를 반영한다.
     * 재검증 실패 시에는 캐시 전체를 그대로 사용한다.
     */
    override suspend fun invoke(shopId: String): RamapResult<ShopDetail> {
        val cached = cache[shopId]
        if (cached != null) return revalidateDetail(cached)

        val result = ramenShopRepository.fetchShopDetail(shopId)
        if (result is RamapResult.Success) cache[result.data.shop.id] = result.data
        return result
    }

    override fun findCached(shopId: String): ShopDetailCacheLookup =
        cache[shopId]
            ?.let(ShopDetailCacheLookup::Hit)
            ?: ShopDetailCacheLookup.Miss

    override fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    ) {
        val likeCountDelta = if (enabled) 1L else -1L
        cache[shopId]?.let { detail ->
            cache[shopId] = detail.copy(likeCount = (detail.likeCount + likeCountDelta).coerceAtLeast(0L))
        }
    }

    /**
     * 캐시된 매장·웨이팅은 유지하고 이벤트·공지·메뉴만 새로 조회해 상세를 갱신한다.
     */
    private suspend fun revalidateDetail(cached: ShopDetail): RamapResult<ShopDetail> {
        val refreshed =
            when (val result = ramenShopRepository.fetchShopDetail(cached.shop.id)) {
                is RamapResult.Success -> result.data
                is RamapResult.Error -> return RamapResult.Success(cached)
            }
        val updated =
            cached.copy(
                event = refreshed.event,
                operatingNotice = refreshed.operatingNotice,
                menuSections = refreshed.menuSections,
                reviews = refreshed.reviews,
            )
        cache[cached.shop.id] = updated
        return RamapResult.Success(updated)
    }
}
