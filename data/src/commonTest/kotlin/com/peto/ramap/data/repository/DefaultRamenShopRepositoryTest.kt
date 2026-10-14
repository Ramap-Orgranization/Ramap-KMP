package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.MenuResponse
import com.peto.ramap.data.model.MenuSectionResponse
import com.peto.ramap.data.model.ShopDetailResponse
import com.peto.ramap.data.model.ShopEventParticipantResponse
import com.peto.ramap.data.model.ShopEventResponse
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.model.shop.WaitingProvider
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.fake.FakeRamenShopDataSource
import com.peto.ramap.fixture.BOUNDS_FIXTURE
import com.peto.ramap.fixture.ramenShopResponseFixture
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultRamenShopRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `상세 RPC의 매장 컬럼에서 웨이팅을 읽고 기존 중첩 응답은 무시한다`() =
        runTest {
            val response =
                json.decodeFromString<ShopDetailResponse>(
                    """
                    {
                        "shop": {
                            "id": "shop-1",
                            "name": "라멘집",
                            "address": "서울",
                            "lat": 37.5,
                            "lng": 127.0,
                            "created_at": "2026-06-01T00:00:00Z",
                            "updated_at": "2026-06-02T00:00:00Z",
                            "waiting_provider": "catchtable",
                            "waiting_provider_url": "https://app.catchtable.co.kr/ct/shop/shop-1"
                        },
                        "like_count": 3,
                        "waiting_system": {
                            "id": "old-waiting",
                            "shop_id": "shop-1",
                            "provider": "tabling",
                            "provider_url": "https://example.com/old-waiting"
                        }
                    }
                    """.trimIndent(),
                )
            val repository = DefaultRamenShopRepository(FakeRamenShopDataSource(shopDetailResponse = response))

            val detail = repository.fetchShopDetail("shop-1").getOrThrow()

            assertEquals(
                WaitingSystem(
                    id = "shop-1",
                    shopId = "shop-1",
                    provider = WaitingProvider.CATCHTABLE,
                    providerUrl = "https://app.catchtable.co.kr/ct/shop/shop-1",
                ),
                detail.waitingSystem,
            )
            assertEquals(3L, detail.likeCount)
            assertEquals("shop-1", detail.shop.id)
        }

    @Test
    fun `상세 웨이팅은 모든 제공자와 알 수 없는 제공자 및 URL 없음을 유지한다`() =
        runTest {
            val providers =
                listOf(
                    "catchtable" to WaitingProvider.CATCHTABLE,
                    "tabling" to WaitingProvider.TABLING,
                    "syrup_friends" to WaitingProvider.SYRUP_FRIENDS,
                    "new_provider" to WaitingProvider.UNKNOWN,
                )
            for ((providerId, expectedProvider) in providers) {
                val response =
                    ShopDetailResponse(
                        shop = ramenShopResponseFixture(id = "shop-1").copy(waitingProvider = providerId),
                        likeCount = 0L,
                    )
                val repository = DefaultRamenShopRepository(FakeRamenShopDataSource(shopDetailResponse = response))

                val detail = repository.fetchShopDetail("shop-1").getOrThrow()

                assertEquals(
                    WaitingSystem(
                        id = "shop-1",
                        shopId = "shop-1",
                        provider = expectedProvider,
                        providerUrl = null,
                    ),
                    detail.waitingSystem,
                )
            }
        }

    @Test
    fun `상세 매장에 웨이팅 컬럼이 없으면 웨이팅 없이 조회한다`() =
        runTest {
            val response =
                json.decodeFromString<ShopDetailResponse>(
                    """
                    {
                        "shop": {
                            "id": "shop-1",
                            "name": "라멘집",
                            "address": "서울",
                            "lat": 37.5,
                            "lng": 127.0,
                            "created_at": "2026-06-01T00:00:00Z",
                            "updated_at": "2026-06-02T00:00:00Z"
                        },
                        "like_count": 0,
                        "waiting_system": null
                    }
                    """.trimIndent(),
                )
            val repository = DefaultRamenShopRepository(FakeRamenShopDataSource(shopDetailResponse = response))

            val detail = repository.fetchShopDetail("shop-1").getOrThrow()

            assertEquals(null, detail.waitingSystem)
        }

    @Test
    fun `상세 매장에 URL만 있고 웨이팅 제공자가 null이면 웨이팅 없음으로 유지한다`() =
        runTest {
            val response =
                ShopDetailResponse(
                    shop =
                        ramenShopResponseFixture(id = "shop-1").copy(
                            waitingProvider = null,
                            waitingProviderUrl = "https://example.com/waiting",
                        ),
                    likeCount = 0L,
                )
            val repository = DefaultRamenShopRepository(FakeRamenShopDataSource(shopDetailResponse = response))

            val detail = repository.fetchShopDetail("shop-1").getOrThrow()

            assertEquals(null, detail.waitingSystem)
        }

    @Test
    fun `상세 RPC 응답도 기존 이벤트와 메뉴 규칙으로 변환한다`() =
        runTest {
            val response =
                ShopDetailResponse(
                    shop = ramenShopResponseFixture(id = "venue-shop"),
                    likeCount = 3L,
                    events = listOf(shopEventResponse()),
                    eventParticipants =
                        listOf(
                            ShopEventParticipantResponse(eventId = "event", shopId = "partner-shop"),
                            ShopEventParticipantResponse(eventId = "event", shopId = null),
                        ),
                    menuSections =
                        listOf(
                            MenuSectionResponse(
                                id = "section",
                                shopId = "venue-shop",
                                title = "한정 메뉴",
                                description = "하루 10그릇 한정",
                                displayOrder = 0,
                            ),
                        ),
                    menuItems =
                        listOf(
                            MenuResponse(
                                id = "menu",
                                sectionId = "section",
                                name = "시오라멘",
                                priceKrw = 10000,
                                displayOrder = 0,
                            ),
                        ),
                )
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(shopDetailResponse = response),
                )

            val detail = repository.fetchShopDetail("venue-shop").getOrThrow()

            assertEquals(2, detail.event?.collaborationPartnerCount)
            assertEquals("하루 10그릇 한정", detail.menuSections.single().description)
        }

    @Test
    fun `메뉴가 있는 상세는 메뉴 전용 갱신 시각을 반환한다`() =
        runTest {
            val response =
                ShopDetailResponse(
                    shop = ramenShopResponseFixture(id = "menu-update-shop"),
                    likeCount = 0L,
                    menuUpdatedAt = "2026-09-02T12:00:00Z",
                    menuSections =
                        listOf(
                            MenuSectionResponse(
                                id = "section",
                                shopId = "menu-update-shop",
                                title = "상시메뉴",
                                displayOrder = 0,
                            ),
                        ),
                    menuItems =
                        listOf(
                            MenuResponse(
                                id = "menu",
                                sectionId = "section",
                                name = "쇼유라멘",
                                displayOrder = 0,
                            ),
                        ),
                )
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(
                        shopDetailResponse = response,
                    ),
                )

            val detail = repository.fetchShopDetail("menu-update-shop").getOrThrow()

            assertEquals("2026-09-02T12:00:00Z", detail.menuUpdatedAt)
        }

    @Test
    fun `이벤트 메뉴 섹션은 상시 메뉴보다 먼저 표시한다`() =
        runTest {
            val response =
                ShopDetailResponse(
                    shop = ramenShopResponseFixture(id = "menu-order-shop"),
                    likeCount = 0L,
                    menuSections =
                        listOf(
                            MenuSectionResponse(
                                id = "permanent",
                                shopId = "menu-order-shop",
                                title = " 상시메뉴 ",
                                displayOrder = 0,
                            ),
                            MenuSectionResponse(
                                id = "event",
                                shopId = "menu-order-shop",
                                title = "기간 한정",
                                displayOrder = 1,
                            ),
                        ),
                    menuItems =
                        listOf(
                            MenuResponse(
                                id = "permanent-item",
                                sectionId = "permanent",
                                name = "쇼유라멘",
                                displayOrder = 0,
                            ),
                            MenuResponse(
                                id = "event-item",
                                sectionId = "event",
                                name = "한정 라멘",
                                displayOrder = 0,
                            ),
                        ),
                )
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(shopDetailResponse = response),
                )

            val detail = repository.fetchShopDetail("menu-order-shop").getOrThrow()

            assertEquals(listOf("event", "permanent"), detail.menuSections.map { it.id })
        }

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
    fun `활성 이벤트 목록에 잘못된 타입이 포함되어 있으면 실패한다`() =
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

            val result = repository.fetchActiveEvents()

            assertTrue(result is RamapResult.Error)
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
                                ShopEventParticipantResponse(eventId = "event", shopId = "partner-shop"),
                                ShopEventParticipantResponse(eventId = "event", shopId = null),
                            ),
                    ),
                )

            val event = repository.fetchActiveShopEvent("venue-shop").getOrThrow()

            assertEquals(2, event?.collaborationPartnerCount)
            assertEquals(null, event?.upcomingCollaborationPartnerName)
        }

    @Test
    fun `상세 RPC의 다른 활성 이벤트 참여자는 선택한 콜라보 인원에서 제외한다`() =
        runTest {
            val response =
                ShopDetailResponse(
                    shop = ramenShopResponseFixture(id = "venue-shop"),
                    likeCount = 0L,
                    events = listOf(shopEventResponse(id = "selected-collab")),
                    eventParticipants =
                        listOf(
                            ShopEventParticipantResponse(
                                eventId = "selected-collab",
                                shopId = "selected-partner",
                            ),
                            ShopEventParticipantResponse(
                                eventId = "another-active-event",
                                shopId = "unrelated-partner",
                            ),
                            ShopEventParticipantResponse(
                                eventId = "another-active-event",
                                shopId = null,
                            ),
                        ),
                )
            val repository =
                DefaultRamenShopRepository(
                    FakeRamenShopDataSource(shopDetailResponse = response),
                )

            val detail = repository.fetchShopDetail("venue-shop").getOrThrow()

            assertEquals(1, detail.event?.collaborationPartnerCount)
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
                                name = "숨은 라멘집",
                                kakaoPlaceUrl = null,
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
                        name = "라멘집",
                        address = "서울시 마포구 라멘로 1",
                        location = Location(lat = 37.551, lng = 126.921),
                        kakaoPlaceUrl = "https://place.map.kakao.com/shop-1",
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
                        name = "숨은 라멘집",
                        address = "서울시 마포구 라멘로 1",
                        location = Location(lat = 37.551, lng = 126.921),
                        kakaoPlaceUrl = null,
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
                        name = "시오라멘",
                        address = "서울시 마포구 라멘로 1",
                        location = Location(lat = 37.551, lng = 126.921),
                        kakaoPlaceUrl = "https://place.map.kakao.com/shop-1",
                        instagramUrl = "https://instagram.com/ramen_shop",
                        menuCategories = MenuCategories(listOf(Category.SHIO)),
                        isVisible = true,
                        createdAt = "2026-06-01T00:00:00Z",
                        updatedAt = "2026-06-02T00:00:00Z",
                    ),
                    RamenShop(
                        id = "shop-2",
                        name = "시오 라멘 연구소",
                        address = "서울시 마포구 라멘로 1",
                        location = Location(lat = 37.551, lng = 126.921),
                        kakaoPlaceUrl = "https://place.map.kakao.com/shop-1",
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

    private fun shopEventResponse(
        id: String = "event",
        eventType: String = "collab",
        startDate: String = "2026-07-15",
        endDate: String? = "2026-07-15",
    ) = ShopEventResponse(
        id = id,
        eventType = eventType,
        title = "콜라보",
        description = "설명",
        startDate = startDate,
        endDate = endDate,
        sourceUrl = "https://instagram.com/p/event",
        isToday = false,
        isVenue = true,
        venueShop = ramenShopResponseFixture(id = "venue-shop", name = "요아케"),
        collaboratorShops = listOf(ramenShopResponseFixture(id = "partner-shop", name = "라멘롱시즌")),
    )
}
