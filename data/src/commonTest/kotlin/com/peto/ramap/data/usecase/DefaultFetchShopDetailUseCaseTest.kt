package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.event.EventVenue
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.menu.Menus
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import com.peto.ramap.fake.FakeOperatingNoticeRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fake.FakeReviewRepository
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

class DefaultFetchShopDetailUseCaseTest {
    @Test
    fun `세션 변경 시 캐시 삭제는 이전 계정 비공개 리뷰를 재사용하지 않는다`() =
        runTest {
            val initial = detail()
            val reviews = FakeReviewRepository(reviews = listOf(review(initial.shop.id).copy(isPublic = false)))
            val useCase =
                DefaultFetchShopDetailUseCase(
                    FakeRamenShopRepository(shopDetail = initial),
                    FakeOperatingNoticeRepository(),
                    reviews,
                )
            useCase(initial.shop.id)
            assertIs<ShopDetailCacheLookup.Hit>(useCase.findCached(initial.shop.id))

            useCase.clearCache()
            reviews.error = RamapError.Unknown(IllegalStateException("offline"))
            assertIs<ShopDetailCacheLookup.Miss>(useCase.findCached(initial.shop.id))
            val loaded = assertIs<RamapResult.Success<ShopDetail>>(useCase(initial.shop.id)).data
            assertEquals(emptyList(), loaded.reviews)
        }

    @Test
    fun `최초 조회와 캐시 재검증은 각각 상세 RPC를 한 번 요청한다`() =
        runTest {
            val initial = detail()
            val repository = FakeRamenShopRepository(shopDetail = initial)
            val useCase =
                DefaultFetchShopDetailUseCase(
                    repository,
                    FakeOperatingNoticeRepository(),
                    FakeReviewRepository(),
                )

            useCase(initial.shop.id)
            useCase(initial.shop.id)

            assertEquals(listOf(initial.shop.id, initial.shop.id), repository.requestedShopDetailIds)
        }

    @Test
    fun `최초 상세 조회는 리뷰를 함께 불러온다`() =
        runTest {
            val initial = detail()
            val review = review(initial.shop.id)
            val reviews = FakeReviewRepository(reviews = listOf(review))
            val useCase =
                DefaultFetchShopDetailUseCase(FakeRamenShopRepository(shopDetail = initial), FakeOperatingNoticeRepository(), reviews)

            val result = assertIs<RamapResult.Success<ShopDetail>>(useCase(initial.shop.id)).data

            assertEquals(listOf(review), result.reviews)
            assertEquals(1, result.reviewCount)
            assertEquals(0, result.menuItemCount)
            assertEquals(listOf(initial.shop.id to 0L), reviews.requestedShopReviews)
        }

    @Test
    fun `캐시 재검증은 리뷰를 갱신하고 리뷰 조회 실패 시 이전 목록을 유지한다`() =
        runTest {
            val initial = detail()
            val firstReview = review(initial.shop.id)
            val secondReview = firstReview.copy(id = "review-2")
            val reviews = FakeReviewRepository(reviews = listOf(firstReview))
            val useCase =
                DefaultFetchShopDetailUseCase(FakeRamenShopRepository(shopDetail = initial), FakeOperatingNoticeRepository(), reviews)
            useCase(initial.shop.id)
            reviews.reviews = listOf(secondReview)

            val refreshed = assertIs<RamapResult.Success<ShopDetail>>(useCase(initial.shop.id)).data
            assertEquals(listOf(secondReview), refreshed.reviews)

            reviews.error = RamapError.Unknown(IllegalStateException("failed"))
            val fallback = assertIs<RamapResult.Success<ShopDetail>>(useCase(initial.shop.id)).data
            assertEquals(listOf(secondReview), fallback.reviews)
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
            val useCase =
                DefaultFetchShopDetailUseCase(
                    repository,
                    FakeOperatingNoticeRepository(),
                    FakeReviewRepository(),
                )
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
            val useCase =
                DefaultFetchShopDetailUseCase(
                    repository,
                    FakeOperatingNoticeRepository(),
                    FakeReviewRepository(),
                )
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
            val useCase =
                DefaultFetchShopDetailUseCase(
                    repository,
                    FakeOperatingNoticeRepository(),
                    FakeReviewRepository(),
                )

            val result = useCase("shop")

            assertIs<RamapResult.Error>(result)
            assertEquals(listOf("shop"), repository.requestedShopDetailIds)
            assertIs<ShopDetailCacheLookup.Miss>(useCase.findCached("shop"))
        }

    @Test
    fun `캐시된 상세의 좋아요 수를 저장 상태 변경에 맞춰 갱신한다`() =
        runTest {
            val initial = detail(likeCount = 1L)
            val useCase =
                DefaultFetchShopDetailUseCase(
                    FakeRamenShopRepository(shopDetail = initial),
                    FakeOperatingNoticeRepository(),
                    FakeReviewRepository(),
                )
            useCase(initial.shop.id)

            useCase.updateCachedLikeCount(initial.shop.id, enabled = false)
            useCase.updateCachedLikeCount(initial.shop.id, enabled = false)

            val cached = assertIs<ShopDetailCacheLookup.Hit>(useCase.findCached(initial.shop.id)).detail
            assertEquals(0L, cached.likeCount)
        }

    @Test
    fun `만료된 공지 다음의 진행 중 공지를 대표 공지로 선택한다`() =
        runTest {
            val shopDetail = detail()
            val today = seoulToday()
            val expired =
                operatingNotice(
                    "expired",
                    shopDetail,
                    today.minus(2, DateTimeUnit.DAY),
                    today.minus(1, DateTimeUnit.DAY),
                )
            val current = operatingNotice("current", shopDetail, today, today)
            val useCase =
                DefaultFetchShopDetailUseCase(
                    FakeRamenShopRepository(shopDetail = shopDetail),
                    FakeOperatingNoticeRepository(notices = listOf(expired, current)),
                    FakeReviewRepository(),
                )

            val result = assertIs<RamapResult.Success<ShopDetail>>(useCase(shopDetail.shop.id)).data

            assertEquals(current, result.operatingNotice)
            assertEquals(listOf(expired, current), result.operatingNotices)
        }

    @Test
    fun `만료된 공지만 있으면 대표 공지를 비운다`() =
        runTest {
            val shopDetail = detail()
            val today = seoulToday()
            val expired =
                operatingNotice(
                    "expired",
                    shopDetail,
                    today.minus(2, DateTimeUnit.DAY),
                    today.minus(1, DateTimeUnit.DAY),
                )
            val useCase =
                DefaultFetchShopDetailUseCase(
                    FakeRamenShopRepository(shopDetail = shopDetail),
                    FakeOperatingNoticeRepository(notices = listOf(expired)),
                    FakeReviewRepository(),
                )

            val result = assertIs<RamapResult.Success<ShopDetail>>(useCase(shopDetail.shop.id)).data

            assertNull(result.operatingNotice)
            assertEquals(listOf(expired), result.operatingNotices)
        }

    @Test
    fun `종료일이 오늘인 공지를 대표 공지로 선택한다`() =
        runTest {
            val shopDetail = detail()
            val today = seoulToday()
            val endingToday =
                operatingNotice(
                    "ending-today",
                    shopDetail,
                    today.minus(1, DateTimeUnit.DAY),
                    today,
                )
            val useCase =
                DefaultFetchShopDetailUseCase(
                    FakeRamenShopRepository(shopDetail = shopDetail),
                    FakeOperatingNoticeRepository(notices = listOf(endingToday)),
                    FakeReviewRepository(),
                )

            val result = assertIs<RamapResult.Success<ShopDetail>>(useCase(shopDetail.shop.id)).data

            assertEquals(endingToday, result.operatingNotice)
        }

    @Test
    fun `예정 공지를 대표 공지로 선택한다`() =
        runTest {
            val shopDetail = detail()
            val today = seoulToday()
            val scheduled =
                operatingNotice(
                    "scheduled",
                    shopDetail,
                    today.plus(1, DateTimeUnit.DAY),
                    today.plus(2, DateTimeUnit.DAY),
                )
            val useCase =
                DefaultFetchShopDetailUseCase(
                    FakeRamenShopRepository(shopDetail = shopDetail),
                    FakeOperatingNoticeRepository(notices = listOf(scheduled)),
                    FakeReviewRepository(),
                )

            val result = assertIs<RamapResult.Success<ShopDetail>>(useCase(shopDetail.shop.id)).data

            assertEquals(scheduled, result.operatingNotice)
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

    private fun operatingNotice(
        id: String,
        shopDetail: ShopDetail,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = OperatingNotice(
        id = id,
        shop = shopDetail.shop,
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
