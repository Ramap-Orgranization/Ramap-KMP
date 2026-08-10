package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.fake.FakeRamenShopDataSource
import com.peto.ramap.fixture.BOUNDS_FIXTURE
import com.peto.ramap.fixture.ramenShopResponseFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRamenShopRepositoryTest {
    @Test
    fun `활성 이벤트가 없으면 성공 결과에 null을 반환한다`() =
        runTest {
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(activeEventsResponses = emptyList()),
                )

            val result = repository.fetchActiveEvent("missing-event")

            assertEquals(RamapResult.Success(null), result)
        }

    @Test
    fun `활성 이벤트 목록은 잘못된 타입을 제외하고 응답 순서를 유지한다`() =
        runTest {
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(
                        activeEventsResponses =
                            listOf(
                                shopEventResponse().copy(id = "today", isToday = true),
                                shopEventResponse().copy(id = "invalid", eventType = "unknown"),
                                shopEventResponse().copy(id = "upcoming"),
                            ),
                    ),
                )

            val events = repository.fetchActiveEvents().getOrThrow()

            assertEquals(listOf("today", "upcoming"), events.map { it.id })
        }

    @Test
    fun `한 콜라보에 상대가 여러 명이면 특정 매장명을 노출하지 않는다`() =
        runTest {
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(
                        activeEventResponses = listOf(shopEventResponse()),
                        participantResponses =
                            listOf(
                                ShopEventParticipantResponse(shopId = "partner-shop"),
                                ShopEventParticipantResponse(shopId = null),
                            ),
                    ),
                )

            val event = repository.fetchActiveShopEvent("venue-shop").getOrThrow()

            assertEquals(2, event?.collaborationPartnerCount)
            assertEquals(null, event?.upcomingCollaborationPartnerName)
        }

    @Test
    fun `라멘 가게 목록을 조회하면 도메인 모델로 변환한다`() =
        runTest {
            val dataSource =
                FakeRamenShopDataSource(
                    responses =
                        listOf(
                            ramenShopResponseFixture(
                                id = "shop-1",
                                name = "라멘집",
                                menuCategoryIds = listOf("shoyu", "tonkotsu", "chanke", "unknown"),
                            ),
                            ramenShopResponseFixture(
                                id = "shop-2",
                                kakaoPlaceId = null,
                                name = "숨은 라멘집",
                                kakaoPlaceUrl = null,
                                phone = null,
                                instagramUrl = null,
                                menuCategoryIds = null,
                            ),
                        ),
                )
            val repository = DefaultRamenShopRepository(dataSource)

            val result = repository.fetchRamenShops(BOUNDS_FIXTURE).getOrThrow()

            assertEquals(BOUNDS_FIXTURE, dataSource.requestedBounds)
            assertEquals(
                listOf(
                    RamenShop(
                        id = "shop-1",
                        kakaoPlaceId = "kakao-shop-1",
                        name = "라멘집",
                        address = "서울시 마포구 라멘로 1",
                        location = Location(lat = 37.551, lng = 126.921),
                        kakaoPlaceUrl = "https://place.map.kakao.com/shop-1",
                        phone = "02-0000-0000",
                        instagramUrl = "https://instagram.com/ramen_shop",
                        menuCategories =
                            MenuCategories(
                                listOf(Category.SHOYU, Category.TONKOTSU, Category.CHANKE),
                            ),
                        isVisible = true,
                        createdAt = "2026-06-01T00:00:00Z",
                        updatedAt = "2026-06-02T00:00:00Z",
                    ),
                    RamenShop(
                        id = "shop-2",
                        kakaoPlaceId = null,
                        name = "숨은 라멘집",
                        address = "서울시 마포구 라멘로 1",
                        location = Location(lat = 37.551, lng = 126.921),
                        kakaoPlaceUrl = null,
                        phone = null,
                        instagramUrl = null,
                        menuCategories = MenuCategories(emptyList()),
                        isVisible = true,
                        createdAt = "2026-06-01T00:00:00Z",
                        updatedAt = "2026-06-02T00:00:00Z",
                    ),
                ).associateBy { it.id },
                result.toMap(),
            )
        }

    @Test
    fun `라멘 가게 목록이 없으면 빈 목록을 반환한다`() =
        runTest {
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(responses = emptyList()),
                )

            val result = repository.fetchRamenShops(BOUNDS_FIXTURE).getOrThrow()

            assertEquals(emptyMap(), result.toMap())
        }

    @Test
    fun `라멘 가게를 검색하면 요청 조건을 전달하고 도메인 모델로 변환한다`() =
        runTest {
            val query = SearchQuery("시오")
            val limit = 5
            val dataSource =
                FakeRamenShopDataSource(
                    searchResponses =
                        listOf(
                            ramenShopResponseFixture(
                                id = "shop-1",
                                name = "시오라멘",
                                menuCategoryIds = listOf("shio", "unknown"),
                            ),
                            ramenShopResponseFixture(
                                id = "shop-2",
                                name = "시오 라멘 연구소",
                                menuCategoryIds = null,
                            ),
                        ),
                )
            val repository = DefaultRamenShopRepository(dataSource)

            val result = repository.searchRamenShops(query, limit).getOrThrow()

            assertEquals(query, dataSource.requestedSearchQuery)
            assertEquals(limit, dataSource.requestedSearchLimit)
            assertEquals(
                listOf(
                    RamenShop(
                        id = "shop-1",
                        kakaoPlaceId = "kakao-shop-1",
                        name = "시오라멘",
                        address = "서울시 마포구 라멘로 1",
                        location = Location(lat = 37.551, lng = 126.921),
                        kakaoPlaceUrl = "https://place.map.kakao.com/shop-1",
                        phone = "02-0000-0000",
                        instagramUrl = "https://instagram.com/ramen_shop",
                        menuCategories = MenuCategories(listOf(Category.SHIO)),
                        isVisible = true,
                        createdAt = "2026-06-01T00:00:00Z",
                        updatedAt = "2026-06-02T00:00:00Z",
                    ),
                    RamenShop(
                        id = "shop-2",
                        kakaoPlaceId = "kakao-shop-1",
                        name = "시오 라멘 연구소",
                        address = "서울시 마포구 라멘로 1",
                        location = Location(lat = 37.551, lng = 126.921),
                        kakaoPlaceUrl = "https://place.map.kakao.com/shop-1",
                        phone = "02-0000-0000",
                        instagramUrl = "https://instagram.com/ramen_shop",
                        menuCategories = MenuCategories(emptyList()),
                        isVisible = true,
                        createdAt = "2026-06-01T00:00:00Z",
                        updatedAt = "2026-06-02T00:00:00Z",
                    ),
                ).associateBy { it.id },
                result.toMap(),
            )
        }

    @Test
    fun `검색 결과가 없으면 빈 목록을 반환한다`() =
        runTest {
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(searchResponses = emptyList()),
                )

            val result = repository.searchRamenShops(SearchQuery("없음"), limit = 5).getOrThrow()

            assertEquals(emptyMap(), result.toMap())
        }

    private fun shopEventResponse() =
        ShopEventResponse(
            id = "event",
            eventType = "collab",
            title = "콜라보",
            description = "설명",
            startDate = "2026-07-15",
            endDate = "2026-07-15",
            sourceUrl = "https://instagram.com/p/event",
            isToday = false,
            isVenue = true,
            venueShopId = "venue-shop",
            venueShopName = "요아케",
            venueAddress = "서울",
            collaboratorShopId = "partner-shop",
            collaboratorName = "라멘롱시즌",
        )
}
