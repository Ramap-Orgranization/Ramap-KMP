package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
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
                )
            val waitingRepository =
                FakeShopWaitingSystemRepository(waitingSystemFixture(shop.id))
            val useCase =
                DefaultFetchShopDetailUseCase(
                    ramenShopRepository,
                    waitingRepository,
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
        }

    @Test
    fun `상세 조회 실패는 캐시하지 않고 한 번 재시도한다`() =
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
                )

            val result = useCase(shop.id)

            assertIs<RamapResult.Error>(result)
            assertEquals(
                listOf(setOf(shop.id), setOf(shop.id)),
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
                )

            val result = useCase(shop.id)

            val detail = assertIs<ShopDetail>(assertIs<RamapResult.Success<*>>(result).data)
            assertNull(detail.event)
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
}
