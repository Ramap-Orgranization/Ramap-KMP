package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.usecase.ShopDetail
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
    fun `아이디로 조회한 상세는 캐시해 다시 요청하지 않는다`() =
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
            assertEquals(listOf(setOf(shop.id)), ramenShopRepository.requestedShopIdsHistory)
            assertEquals(listOf(shop.id), waitingRepository.requestedShopIds)
            assertEquals(listOf(shop.id), ramenShopRepository.requestedActiveEventShopIds)
            assertEquals(shop.id, useCase.findCached(shop.id)?.shop?.id)
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
            assertNull(useCase.findCached(shop.id))
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
            assertEquals(null, detail.event)
        }
}
