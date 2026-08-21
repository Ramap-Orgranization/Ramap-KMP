package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class DefaultFetchShopDetailUseCase(
    private val ramenShopRepository: RamenShopRepository,
    private val waitingSystemRepository: ShopWaitingSystemRepository,
    private val operatingNoticeRepository: OperatingNoticeRepository,
) : FetchShopDetailUseCase {
    private val cache = mutableMapOf<String, ShopDetail>()

    /**
     * 매장 상세를 조회한다.
     *
     * 캐시에 매장·웨이팅 정보가 있으면 재사용하고 이벤트만 새로 조회해
     * 종료·신규 이벤트가 즉시 반영되도록 한다.
     * 이벤트 재조회 실패는 캐시된 이벤트를 그대로 사용한다.
     */
    override suspend fun invoke(shopId: String): RamapResult<ShopDetail> {
        val cached = cache[shopId]
        if (cached != null) return revalidateEvent(cached)

        val result = loadFresh(shopId)
        if (result is RamapResult.Success) cache[result.data.shop.id] = result.data
        return result
    }

    override fun findCached(shopId: String): ShopDetailCacheLookup =
        cache[shopId]
            ?.let(ShopDetailCacheLookup::Hit)
            ?: ShopDetailCacheLookup.Miss

    /**
     * 캐시된 매장·웨이팅은 유지하고 이벤트만 새로 조회해 상세를 갱신한다.
     */
    private suspend fun revalidateEvent(cached: ShopDetail): RamapResult<ShopDetail> {
        val eventResult = ramenShopRepository.fetchActiveShopEvent(cached.shop.id)
        val noticeResult = operatingNoticeRepository.fetchActiveShopOperatingNotice(cached.shop.id)
        val updated =
            cached.copy(
                event =
                    when (eventResult) {
                        is RamapResult.Success -> eventResult.data
                        is RamapResult.Error -> cached.event
                    },
                operatingNotice =
                    when (noticeResult) {
                        is RamapResult.Success -> noticeResult.data
                        is RamapResult.Error -> cached.operatingNotice
                    },
            )
        cache[cached.shop.id] = updated
        return RamapResult.Success(updated)
    }

    private suspend fun loadFresh(shopId: String): RamapResult<ShopDetail> =
        coroutineScope {
            val shopResult = async { ramenShopRepository.fetchRamenShops(setOf(shopId)) }
            val likeCountResult = async { ramenShopRepository.fetchShopLikeCount(shopId) }
            val waitingResult = async { waitingSystemRepository.fetchShopWaitingSystem(shopId) }
            val eventResult = async { ramenShopRepository.fetchActiveShopEvent(shopId) }
            val noticeResult = async { operatingNoticeRepository.fetchActiveShopOperatingNotice(shopId) }

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
                            val notice = noticeResult.await()
                            val activeNotice =
                                if (notice is RamapResult.Success) {
                                    notice.data
                                } else {
                                    null
                                }
                            RamapResult.Success(
                                ShopDetail(
                                    shop = shop,
                                    likeCount = (likeCountResult.await() as? RamapResult.Success)?.data ?: 0L,
                                    waitingSystem = waiting.data,
                                    event = activeEvent,
                                    operatingNotice = activeNotice,
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
