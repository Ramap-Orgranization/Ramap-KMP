package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.ShopWaitingSystemResponse
import com.peto.ramap.domain.model.shop.WaitingProvider
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.fake.FakeShopWaitingSystemDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultWaitingSystemRepositoryTest {
    @Test
    fun `웨이팅 시스템을 조회하면 도메인 모델로 변환한다`() =
        runTest {
            val dataSource =
                FakeShopWaitingSystemDataSource(
                    response =
                        ShopWaitingSystemResponse(
                            provider = "catchtable",
                            providerUrl = "https://app.catchtable.co.kr/ct/shop/shop-1",
                        ),
                )
            val repository = DefaultShopWaitingSystemRepository(dataSource)

            val result = repository.fetchShopWaitingSystem("shop-1").getOrThrow()

            assertEquals("shop-1", dataSource.requestedShopId)
            assertEquals(
                WaitingSystem(
                    id = "shop-1",
                    shopId = "shop-1",
                    provider = WaitingProvider.CATCHTABLE,
                    providerUrl = "https://app.catchtable.co.kr/ct/shop/shop-1",
                ),
                result,
            )
        }

    @Test
    fun `알 수 없는 웨이팅 provider는 UNKNOWN으로 변환한다`() =
        runTest {
            val repository =
                DefaultShopWaitingSystemRepository(
                    FakeShopWaitingSystemDataSource(
                        response =
                            ShopWaitingSystemResponse(
                                provider = "new_provider",
                            ),
                    ),
                )

            val result = repository.fetchShopWaitingSystem("shop-1").getOrThrow()

            assertEquals(WaitingProvider.UNKNOWN, result?.provider)
        }

    @Test
    fun `웨이팅 시스템이 없으면 null을 반환한다`() =
        runTest {
            val repository =
                DefaultShopWaitingSystemRepository(
                    FakeShopWaitingSystemDataSource(response = ShopWaitingSystemResponse()),
                )

            val result = repository.fetchShopWaitingSystem("shop-1").getOrThrow()

            assertEquals(null, result)
        }

    @Test
    fun `웨이팅 시스템 데이터 소스에서 예외가 발생하면 Unknown 오류로 변환한다`() =
        runTest {
            val expected = IllegalStateException("Failed to fetch waiting system")
            val repository =
                DefaultShopWaitingSystemRepository(
                    FakeShopWaitingSystemDataSource(error = expected),
                )

            val actual = repository.fetchShopWaitingSystem("shop-1")

            assertEquals(RamapResult.Error(RamapError.Unknown(expected)), actual)
        }
}
