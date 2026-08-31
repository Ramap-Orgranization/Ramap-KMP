package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.menu.Menus
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.fixture.waitingSystemFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultFetchShopDetailUseCaseTest {
    @Test
    fun `최초 조회와 캐시 재검증은 각각 상세 RPC를 한 번 요청한다`() =
        runTest {
            val initial = detail()
            val repository = FakeRamenShopRepository(shopDetail = initial)
            val useCase = DefaultFetchShopDetailUseCase(repository)

            useCase(initial.shop.id)
            useCase(initial.shop.id)

            assertEquals(listOf(initial.shop.id, initial.shop.id), repository.requestedShopDetailIds)
        }

    @Test
    fun `캐시 재검증은 매장 좋아요 웨이팅을 유지하고 나머지만 갱신한다`() =
        runTest {
            val initial = detail()
            val refreshed =
                detail(
                    shopId = initial.shop.id,
                    likeCount = 99L,
                    event = event(initial.shop.id),
                    menuSections = listOf(menuSection()),
                )
            val repository = FakeRamenShopRepository(shopDetail = initial)
            val useCase = DefaultFetchShopDetailUseCase(repository)
            useCase(initial.shop.id)
            repository.shopDetail = refreshed

            val result = assertIs<RamapResult.Success<ShopDetail>>(useCase(initial.shop.id)).data

            assertEquals(initial.shop, result.shop)
            assertEquals(initial.likeCount, result.likeCount)
            assertEquals(initial.waitingSystem, result.waitingSystem)
            assertEquals(refreshed.event, result.event)
            assertEquals(refreshed.menuSections, result.menuSections)
        }

    @Test
    fun `캐시 재검증 실패는 캐시 전체를 성공으로 유지한다`() =
        runTest {
            val initial = detail(event = event("shop"))
            val repository = FakeRamenShopRepository(shopDetail = initial)
            val useCase = DefaultFetchShopDetailUseCase(repository)
            useCase(initial.shop.id)
            repository.shopDetailError = RamapError.Unknown(IllegalStateException("failed"))

            val result = assertIs<RamapResult.Success<ShopDetail>>(useCase(initial.shop.id))

            assertEquals(initial, result.data)
        }

    @Test
    fun `최초 상세 RPC 실패는 캐시하지 않는다`() =
        runTest {
            val repository =
                FakeRamenShopRepository(
                    shopDetailError = RamapError.Unknown(IllegalStateException("failed")),
                )
            val useCase = DefaultFetchShopDetailUseCase(repository)

            val result = useCase("shop")

            assertIs<RamapResult.Error>(result)
            assertEquals(listOf("shop"), repository.requestedShopDetailIds)
            assertIs<ShopDetailCacheLookup.Miss>(useCase.findCached("shop"))
        }

    @Test
    fun `캐시된 상세의 좋아요 수를 저장 상태 변경에 맞춰 갱신한다`() =
        runTest {
            val initial = detail(likeCount = 1L)
            val useCase = DefaultFetchShopDetailUseCase(FakeRamenShopRepository(shopDetail = initial))
            useCase(initial.shop.id)

            useCase.updateCachedLikeCount(initial.shop.id, enabled = false)
            useCase.updateCachedLikeCount(initial.shop.id, enabled = false)

            val cached = assertIs<ShopDetailCacheLookup.Hit>(useCase.findCached(initial.shop.id)).detail
            assertEquals(0L, cached.likeCount)
        }

    private fun detail(
        shopId: String = "shop",
        likeCount: Long = 7L,
        event: ShopEvent? = null,
        menuSections: List<MenuSection> = emptyList(),
    ): ShopDetail =
        ShopDetail(
            shop = ramenShopFixture(id = shopId),
            likeCount = likeCount,
            waitingSystem = waitingSystemFixture(shopId),
            event = event,
            operatingNotice = null,
            menuSections = menuSections,
        )

    private fun menuSection() =
        MenuSection(
            id = "section",
            title = "메뉴",
            displayOrder = 0,
            items = Menus(emptyList()),
        )

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
