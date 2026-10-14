package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.event.EventVenue
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.usecase.ShopOverviewCacheLookup
import com.peto.ramap.fake.FakeOperatingNoticeRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.fixture.waitingSystemFixture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock

class DefaultFetchShopOverviewUseCaseTest {
    @Test
    fun `최초 조회와 캐시 재검증은 각각 상세 RPC를 한 번 요청한다`() =
        runTest {
            val initial = overview()
            val repository = FakeRamenShopRepository(shopOverview = initial)
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = repository,
                    operatingNoticeRepository = FakeOperatingNoticeRepository(),
                )

            useCase(initial.shop.id)
            useCase(initial.shop.id)

            assertEquals(listOf(initial.shop.id, initial.shop.id), repository.requestedShopOverviewIds)
        }

    @Test
    fun `캐시 재검증은 매장 좋아요 웨이팅을 유지하고 나머지만 갱신한다`() =
        runTest {
            val initial = overview()
            val refreshed =
                overview(
                    shopId = initial.shop.id,
                    likeCount = 99L,
                    event = event(initial.shop.id),
                )
            val repository = FakeRamenShopRepository(shopOverview = initial)
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = repository,
                    operatingNoticeRepository = FakeOperatingNoticeRepository(),
                )
            useCase(initial.shop.id)
            repository.shopOverview = refreshed

            val result = assertIs<RamapResult.Success<ShopOverView>>(useCase(initial.shop.id)).data

            assertEquals(initial.shop, result.shop)
            assertEquals(initial.likeCount, result.likeCount)
            assertEquals(initial.waitingSystem, result.waitingSystem)
            assertEquals(refreshed.event, result.event)
        }

    @Test
    fun `캐시 재검증 실패는 캐시 전체를 성공으로 유지한다`() =
        runTest {
            val initial = overview(event = event("shop"))
            val repository = FakeRamenShopRepository(shopOverview = initial)
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = repository,
                    operatingNoticeRepository = FakeOperatingNoticeRepository(),
                )
            useCase(initial.shop.id)
            repository.shopOverviewError = RamapError.Unknown(IllegalStateException("failed"))

            val result = assertIs<RamapResult.Success<ShopOverView>>(useCase(initial.shop.id))

            assertEquals(initial, result.data)
        }

    @Test
    fun `최초 상세 RPC 실패는 캐시하지 않는다`() =
        runTest {
            val repository =
                FakeRamenShopRepository(
                    shopOverviewError = RamapError.Unknown(IllegalStateException("failed")),
                )
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = repository,
                    operatingNoticeRepository = FakeOperatingNoticeRepository(),
                )

            val result = useCase("shop")

            assertIs<RamapResult.Error>(result)
            assertEquals(listOf("shop"), repository.requestedShopOverviewIds)
            assertIs<ShopOverviewCacheLookup.Miss>(useCase.findCached("shop"))
        }

    @Test
    fun `캐시된 상세의 좋아요 수를 저장 상태 변경에 맞춰 갱신한다`() =
        runTest {
            val initial = overview(likeCount = 1L)
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = FakeRamenShopRepository(shopOverview = initial),
                    operatingNoticeRepository = FakeOperatingNoticeRepository(),
                )
            useCase(initial.shop.id)

            useCase.updateCachedLikeCount(initial.shop.id, enabled = false)
            useCase.updateCachedLikeCount(initial.shop.id, enabled = false)

            val cached = assertIs<ShopOverviewCacheLookup.Hit>(useCase.findCached(initial.shop.id)).overview
            assertEquals(0L, cached.likeCount)
        }

    @Test
    fun `만료된 공지 다음의 진행 중 공지를 대표 공지로 선택한다`() =
        runTest {
            val shopOverview = overview()
            val today = seoulToday()
            val expired =
                operatingNotice(
                    "expired",
                    shopOverview,
                    today.minus(2, DateTimeUnit.DAY),
                    today.minus(1, DateTimeUnit.DAY),
                )
            val current = operatingNotice("current", shopOverview, today, today)
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = FakeRamenShopRepository(shopOverview = shopOverview),
                    operatingNoticeRepository = FakeOperatingNoticeRepository(notices = listOf(expired, current)),
                )

            val result = assertIs<RamapResult.Success<ShopOverView>>(useCase(shopOverview.shop.id)).data

            assertEquals(current, result.operatingNotice)
            assertEquals(listOf(expired, current), result.operatingNotices)
        }

    @Test
    fun `만료된 공지만 있으면 대표 공지를 비운다`() =
        runTest {
            val shopOverview = overview()
            val today = seoulToday()
            val expired =
                operatingNotice(
                    "expired",
                    shopOverview,
                    today.minus(2, DateTimeUnit.DAY),
                    today.minus(1, DateTimeUnit.DAY),
                )
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = FakeRamenShopRepository(shopOverview = shopOverview),
                    operatingNoticeRepository = FakeOperatingNoticeRepository(notices = listOf(expired)),
                )

            val result = assertIs<RamapResult.Success<ShopOverView>>(useCase(shopOverview.shop.id)).data

            assertNull(result.operatingNotice)
            assertEquals(listOf(expired), result.operatingNotices)
        }

    @Test
    fun `종료일이 오늘인 공지를 대표 공지로 선택한다`() =
        runTest {
            val shopOverview = overview()
            val today = seoulToday()
            val endingToday =
                operatingNotice(
                    "ending-today",
                    shopOverview,
                    today.minus(1, DateTimeUnit.DAY),
                    today,
                )
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = FakeRamenShopRepository(shopOverview = shopOverview),
                    operatingNoticeRepository = FakeOperatingNoticeRepository(notices = listOf(endingToday)),
                )

            val result = assertIs<RamapResult.Success<ShopOverView>>(useCase(shopOverview.shop.id)).data

            assertEquals(endingToday, result.operatingNotice)
        }

    @Test
    fun `예정 공지를 대표 공지로 선택한다`() =
        runTest {
            val shopOverview = overview()
            val today = seoulToday()
            val scheduled =
                operatingNotice(
                    "scheduled",
                    shopOverview,
                    today.plus(1, DateTimeUnit.DAY),
                    today.plus(2, DateTimeUnit.DAY),
                )
            val useCase =
                DefaultFetchShopOverviewUseCase(
                    ramenShopRepository = FakeRamenShopRepository(shopOverview = shopOverview),
                    operatingNoticeRepository = FakeOperatingNoticeRepository(notices = listOf(scheduled)),
                )

            val result = assertIs<RamapResult.Success<ShopOverView>>(useCase(shopOverview.shop.id)).data

            assertEquals(scheduled, result.operatingNotice)
        }

    private fun overview(
        shopId: String = "shop",
        likeCount: Long = 7L,
        event: ShopEvent? = null,
    ): ShopOverView =
        ShopOverView(
            shop = ramenShopFixture(id = shopId),
            likeCount = likeCount,
            waitingSystem = waitingSystemFixture(shopId),
            event = event,
            operatingNotice = null,
        )

    private fun operatingNotice(
        id: String,
        shopOverview: ShopOverView,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = OperatingNotice(
        id = id,
        shop = shopOverview.shop,
        type = OperatingNoticeType.TEMPORARY_CLOSURE,
        description = "임시 휴무",
        startDate = startDate,
        endDate = endDate,
        startTime = null,
        endTime = null,
        sourceUrl = null,
    )

    private fun seoulToday(): LocalDate =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.of("Asia/Seoul"))
            .date

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
            venue = EventVenue.Registered(ramenShopFixture(id = shopId, name = "매장", address = "서울")),
            waitingMethod = null,
            waitingUrl = null,
        )

    private fun review(shopId: String) =
        Review(
            id = "review-1",
            shopId = shopId,
            body = "맛있는 라멘",
            createdAt = "2026-09-29T00:00:00Z",
        )
}
