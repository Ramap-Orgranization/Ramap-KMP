package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import com.peto.ramap.fake.FakeOperatingNoticeRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeShopWaitingSystemRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.fixture.waitingSystemFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DefaultFetchShopDetailUseCaseTest {
    @Test
    fun `아이디로 조회한 상세는 캐시해 매장과 웨이팅을 다시 요청하지 않는다`() =
        runTest {
            val shop = ramenShopFixture()
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                    shopLikeCount = 7L,
                )
            val waitingRepository =
                FakeShopWaitingSystemRepository(waitingSystemFixture(shop.id))
            val useCase =
                DefaultFetchShopDetailUseCase(
                    ramenShopRepository,
                    waitingRepository,
                    FakeOperatingNoticeRepository(),
                )

            useCase(shop.id)
            val secondResult = useCase(shop.id)

            assertIs<RamapResult.Success<*>>(secondResult)
            // 매장·웨이팅은 최초 1회만 조회
            assertEquals(listOf(setOf(shop.id)), ramenShopRepository.requestedShopIdsHistory)
            assertEquals(listOf(shop.id), waitingRepository.requestedShopIds)
            // 이벤트는 매 호출마다 재조회
            assertEquals(listOf(shop.id, shop.id), ramenShopRepository.requestedActiveEventShopIds)
            val lookup = assertIs<ShopDetailCacheLookup.Hit>(useCase.findCached(shop.id))
            assertEquals(shop.id, lookup.detail.shop.id)
            assertEquals(7L, lookup.detail.likeCount)
        }

    @Test
    fun `캐시된 상세의 좋아요 수를 저장 상태 변경에 맞춰 갱신한다`() =
        runTest {
            val shop = ramenShopFixture()
            val useCase =
                DefaultFetchShopDetailUseCase(
                    FakeRamenShopRepository(
                        fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        shopLikeCount = 1L,
                    ),
                    FakeShopWaitingSystemRepository(),
                    FakeOperatingNoticeRepository(),
                )
            useCase(shop.id)

            useCase.updateCachedLikeCount(shop.id, enabled = false)
            useCase.updateCachedLikeCount(shop.id, enabled = false)

            assertEquals(0L, assertIs<ShopDetailCacheLookup.Hit>(useCase.findCached(shop.id)).detail.likeCount)
        }

    @Test
    fun `상세 조회 실패는 캐시하지 않고 한 번만 요청한다`() =
        runTest {
            val shop = ramenShopFixture()
            val ramenShopRepository =
                FakeRamenShopRepository(
                    error = RamapError.Unknown(IllegalStateException("failed")),
                )
            val useCase =
                DefaultFetchShopDetailUseCase(
                    ramenShopRepository,
                    FakeShopWaitingSystemRepository(),
                    FakeOperatingNoticeRepository(),
                )

            val result = useCase(shop.id)

            assertIs<RamapResult.Error>(result)
            assertEquals(
                listOf(setOf(shop.id)),
                ramenShopRepository.requestedShopIdsHistory,
            )
            assertIs<ShopDetailCacheLookup.Miss>(useCase.findCached(shop.id))
        }

    @Test
    fun `이벤트 조회 실패는 상세 조회를 실패시키지 않는다`() =
        runTest {
            val shop = ramenShopFixture()
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                    activeEventError = RamapError.Unknown(IllegalStateException("failed")),
                )
            val useCase =
                DefaultFetchShopDetailUseCase(
                    ramenShopRepository,
                    FakeShopWaitingSystemRepository(),
                    FakeOperatingNoticeRepository(),
                )

            val result = useCase(shop.id)

            val detail = assertIs<ShopDetail>(assertIs<RamapResult.Success<*>>(result).data)
            assertNull(detail.event)
        }

    @Test
    fun `좋아요 수 조회 실패는 상세 조회를 실패시키지 않고 0개로 표시한다`() =
        runTest {
            val shop = ramenShopFixture()
            val useCase =
                DefaultFetchShopDetailUseCase(
                    FakeRamenShopRepository(
                        fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                        shopLikeCountError = RamapError.Unknown(IllegalStateException("failed")),
                    ),
                    FakeShopWaitingSystemRepository(),
                    FakeOperatingNoticeRepository(),
                )

            val result = useCase(shop.id)

            val detail = assertIs<ShopDetail>(assertIs<RamapResult.Success<*>>(result).data)
            assertEquals(0L, detail.likeCount)
        }

    @Test
    fun `캐시 히트 시 이벤트 재조회 실패는 캐시된 이벤트를 유지한다`() =
        runTest {
            val shop = ramenShopFixture()
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                )
            val useCase =
                DefaultFetchShopDetailUseCase(
                    ramenShopRepository,
                    FakeShopWaitingSystemRepository(),
                    FakeOperatingNoticeRepository(),
                )

            // 최초 조회: 이벤트 없음 (null)
            useCase(shop.id)

            // 이벤트 조회 실패로 전환
            ramenShopRepository.activeEventError = RamapError.Unknown(IllegalStateException("failed"))
            val secondResult = useCase(shop.id)

            // 실패해도 캐시된 이벤트(null)를 유지하며 성공 반환
            val detail = assertIs<ShopDetail>(assertIs<RamapResult.Success<*>>(secondResult).data)
            assertNull(detail.event)
        }

    @Test
    fun `캐시 히트 시 활성 이벤트가 없으면 종료된 이벤트를 캐시에서 제거한다`() =
        runTest {
            val shop = ramenShopFixture()
            val event = event(shop.id)
            val ramenShopRepository =
                FakeRamenShopRepository(
                    fetchByIdsResult = RamenShops(mapOf(shop.id to shop)),
                    activeEvent = event,
                )
            val useCase =
                DefaultFetchShopDetailUseCase(
                    ramenShopRepository,
                    FakeShopWaitingSystemRepository(),
                    FakeOperatingNoticeRepository(),
                )

            val firstResult = assertIs<RamapResult.Success<ShopDetail>>(useCase(shop.id))
            assertEquals(event, firstResult.data.event)

            ramenShopRepository.activeEvent = null
            val secondResult = assertIs<RamapResult.Success<ShopDetail>>(useCase(shop.id))

            assertNull(secondResult.data.event)
            assertNull(assertIs<ShopDetailCacheLookup.Hit>(useCase.findCached(shop.id)).detail.event)
        }

    private fun event(shopId: String) =
        ShopEvent(
            id = "event",
            type = ShopEventType.POPUP,
            title = "팝업",
            description = "설명",
            startDate = "2099-01-01",
            endDate = "2099-01-02",
            sourceUrl = "https://example.com/event",
            isToday = false,
            isVenue = true,
            venueShop = ramenShopFixture(id = shopId, name = "매장", address = "서울"),
            waitingMethod = null,
            waitingUrl = null,
        )
}
