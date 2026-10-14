package com.peto.ramap.ui.main

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.retryOnce
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.domain.usecase.FetchShopOverviewUseCase
import com.peto.ramap.domain.usecase.ShopOverviewCacheLookup
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class FakeFetchShopOverviewUseCase(
    private val ramenShopRepository: RamenShopRepository,
    private val waitingSystemRepository: ShopWaitingSystemRepository,
    private val operatingNoticeRepository: OperatingNoticeRepository,
) : FetchShopOverviewUseCase {
    private val cache = mutableMapOf<String, ShopOverView>()

    override fun clearCache() {
        cache.clear()
    }

    override suspend fun invoke(shopId: String): RamapResult<ShopOverView> {
        when (val lookup = findCached(shopId)) {
            is ShopOverviewCacheLookup.Hit -> return RamapResult.Success(lookup.overview)
            ShopOverviewCacheLookup.Miss -> Unit
        }

        val result = retryOnce { fetchDetail(shopId) }
        if (result is RamapResult.Success) cache[shopId] = result.data
        return result
    }

    override fun findCached(shopId: String): ShopOverviewCacheLookup =
        cache[shopId]
            ?.let(ShopOverviewCacheLookup::Hit)
            ?: ShopOverviewCacheLookup.Miss

    override fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    ) {
        val likeCountDelta = if (enabled) 1L else -1L
        cache[shopId]?.let { overview ->
            cache[shopId] = overview.copy(likeCount = (overview.likeCount + likeCountDelta).coerceAtLeast(0L))
        }
    }

    private suspend fun fetchDetail(shopId: String): RamapResult<ShopOverView> =
        coroutineScope {
            val shopsResult = async { ramenShopRepository.fetchRamenShops(setOf(shopId)) }
            val likeCountResult = async { ramenShopRepository.fetchShopLikeCount(shopId) }
            val waitingResult = async { waitingSystemRepository.fetchShopWaitingSystem(shopId) }
            val eventResult = async { ramenShopRepository.fetchActiveShopEvent(shopId) }
            val noticeResult = async { operatingNoticeRepository.fetchActiveShopOperatingNotices(shopId) }
            when (val shops = shopsResult.await()) {
                is RamapResult.Error -> shops
                is RamapResult.Success -> {
                    val shop =
                        shops.data[shopId]
                            ?: return@coroutineScope missingShop(shopId)
                    when (val waiting = waitingResult.await()) {
                        is RamapResult.Error -> waiting
                        is RamapResult.Success -> {
                            val event = eventResult.await()
                            val notice = noticeResult.await()
                            RamapResult.Success(
                                ShopOverView(
                                    shop = shop,
                                    likeCount = (likeCountResult.await() as? RamapResult.Success)?.data ?: 0L,
                                    waitingSystem = waiting.data,
                                    event = (event as? RamapResult.Success)?.data,
                                    operatingNotice = (notice as? RamapResult.Success)?.data?.firstOrNull(),
                                    operatingNotices = (notice as? RamapResult.Success)?.data.orEmpty(),
                                ),
                            )
                        }
                    }
                }
            }
        }

    private fun missingShop(shopId: String): RamapResult.Error =
        RamapResult.Error(
            RamapError.Unknown(
                IllegalStateException("매장 상세를 찾을 수 없습니다: $shopId"),
            ),
        )
}
