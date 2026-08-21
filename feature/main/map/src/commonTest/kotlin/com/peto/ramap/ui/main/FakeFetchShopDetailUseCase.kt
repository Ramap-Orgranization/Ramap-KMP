package com.peto.ramap.ui.main

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.retryOnce
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class FakeFetchShopDetailUseCase(
    private val ramenShopRepository: RamenShopRepository,
    private val waitingSystemRepository: ShopWaitingSystemRepository,
    private val operatingNoticeRepository: OperatingNoticeRepository,
) : FetchShopDetailUseCase {
    private val cache = mutableMapOf<String, ShopDetail>()

    override suspend fun invoke(shopId: String): RamapResult<ShopDetail> {
        when (val lookup = findCached(shopId)) {
            is ShopDetailCacheLookup.Hit -> return RamapResult.Success(lookup.detail)
            ShopDetailCacheLookup.Miss -> Unit
        }

        val result = retryOnce { fetchDetail(shopId) }
        if (result is RamapResult.Success) cache[shopId] = result.data
        return result
    }

    override fun findCached(shopId: String): ShopDetailCacheLookup =
        cache[shopId]
            ?.let(ShopDetailCacheLookup::Hit)
            ?: ShopDetailCacheLookup.Miss

    private suspend fun fetchDetail(shopId: String): RamapResult<ShopDetail> =
        coroutineScope {
            val shopsResult = async { ramenShopRepository.fetchRamenShops(setOf(shopId)) }
            val likeCountResult = async { ramenShopRepository.fetchShopLikeCount(shopId) }
            val waitingResult = async { waitingSystemRepository.fetchShopWaitingSystem(shopId) }
            val eventResult = async { ramenShopRepository.fetchActiveShopEvent(shopId) }
            val noticeResult = async { operatingNoticeRepository.fetchActiveShopOperatingNotice(shopId) }

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
                                ShopDetail(
                                    shop = shop,
                                    likeCount = (likeCountResult.await() as? RamapResult.Success)?.data ?: 0L,
                                    waitingSystem = waiting.data,
                                    event = (event as? RamapResult.Success)?.data,
                                    operatingNotice = (notice as? RamapResult.Success)?.data,
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
