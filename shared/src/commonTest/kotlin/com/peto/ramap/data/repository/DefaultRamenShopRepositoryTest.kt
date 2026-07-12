package com.peto.ramap.data.repository

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MenuCategories
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.SearchQuery
import com.peto.ramap.fake.FakeRamenShopDataSource
import com.peto.ramap.fixture.BOUNDS_FIXTURE
import com.peto.ramap.fixture.ramenShopResponseFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRamenShopRepositoryTest {
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
                                menuCategoryIds = listOf("shoyu", "tonkotsu", "unknown"),
                                isVisible = true,
                            ),
                            ramenShopResponseFixture(
                                id = "shop-2",
                                kakaoPlaceId = null,
                                name = "숨은 라멘집",
                                kakaoPlaceUrl = null,
                                phone = null,
                                businessHours = null,
                                instagramUrl = null,
                                kakaoRating = null,
                                menuCategoryIds = null,
                                isVisible = null,
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
                        businessHours = "11:00-21:00",
                        instagramUrl = "https://instagram.com/ramen_shop",
                        kakaoRating = 4.5,
                        menuCategories = MenuCategories(listOf(Category.SHOYU, Category.TONKOTSU)),
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
                        businessHours = null,
                        instagramUrl = null,
                        kakaoRating = null,
                        menuCategories = MenuCategories(emptyList()),
                        isVisible = false,
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
                                isVisible = null,
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
                        businessHours = "11:00-21:00",
                        instagramUrl = "https://instagram.com/ramen_shop",
                        kakaoRating = 4.5,
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
                        businessHours = "11:00-21:00",
                        instagramUrl = "https://instagram.com/ramen_shop",
                        kakaoRating = 4.5,
                        menuCategories = MenuCategories(emptyList()),
                        isVisible = false,
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
}
