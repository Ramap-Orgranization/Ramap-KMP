package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.retryOnce
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetail
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DefaultFetchShopDetailUseCase(
    private val ramenShopRepository: RamenShopRepository,
    private val waitingSystemRepository: ShopWaitingSystemRepository,
) : FetchShopDetailUseCase {
    private val cache = mutableMapOf<String, ShopDetail>()

    override suspend fun invoke(shopId: String): RamapResult<ShopDetail> {
        findCached(shopId)?.let { return RamapResult.Success(it) }

        val result = retryOnce { loadFresh(shopId) }
        if (result is RamapResult.Success) cache[result.data.shop.id] = result.data
        return result
    }

    override fun findCached(shopId: String): ShopDetail? = cache[shopId]

    private suspend fun loadFresh(shopId: String): RamapResult<ShopDetail> =
        coroutineScope {
            val shopResult = async { ramenShopRepository.fetchRamenShops(setOf(shopId)) }
            val waitingResult = async { waitingSystemRepository.fetchShopWaitingSystem(shopId) }
            val eventResult = async { ramenShopRepository.fetchActiveShopEvent(shopId) }

            when (val shops = shopResult.await()) {
                is RamapResult.Error -> shops
                is RamapResult.Success -> {
                    val shop =
                        shops.data[shopId]
                            ?: return@coroutineScope missingShop(shopId)
                    when (val waiting = waitingResult.await()) {
                        is RamapResult.Error -> waiting
                        is RamapResult.Success -> {
                            val event = eventResult.await()
                            val activeEvent =
                                if (event is RamapResult.Success) {
                                    event.data
                                } else {
                                    null
                                }
                            RamapResult.Success(
                                ShopDetail(
                                    shop = shop,
                                    waitingSystem = waiting.data,
                                    event = activeEvent,
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
