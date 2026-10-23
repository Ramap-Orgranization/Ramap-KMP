package com.peto.ramap.data.repository

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.PlaceSearchCenterRequest
import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.data.model.PlaceSearchResponse
import com.peto.ramap.data.model.PlaceSearchResultKindResponse
import com.peto.ramap.data.model.PlaceSearchResultResponse
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.place.PlaceSearchResultKind
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.fake.FakePlaceSearchDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultPlaceSearchRepositoryTest {
    @Test
    fun `동리 후보의 모호성 정보를 역직렬화하고 구버전 응답은 기본값을 사용한다`() {
        val response =
            Json.decodeFromString<PlaceSearchResponse>(
                """{"results":[{"name":"비토리","address":"경남 사천시 서포면 비토리","lat":34.97,"lng":127.97,"kind":"map_location","is_ambiguous_region":true},{"name":"세종","address":"세종특별자치시","lat":36.48,"lng":127.29,"kind":"map_location"}]}""",
            )

        assertEquals(listOf(true, false), response.results.map { it.toDomain().isAmbiguousRegion })
    }

    @Test
    fun `장소 검색 요청에 검색어와 중심점을 전달하고 도메인 결과로 변환한다`() =
        runTest {
            val dataSource =
                FakePlaceSearchDataSource(
                    PlaceSearchResponse(
                        results =
                            listOf(
                                PlaceSearchResultResponse(
                                    name = "멘야 테스트",
                                    address = "서울시 테스트로 1",
                                    lat = 37.5,
                                    lng = 127.0,
                                    kind = PlaceSearchResultKindResponse.MAP_LOCATION,
                                ),
                            ),
                    ),
                )
            val repository = DefaultPlaceSearchRepository(dataSource)
            val center = Location(lat = 37.4, lng = 126.9)

            val result = repository.search(SearchQuery("멘야"), center).getOrThrow()

            assertEquals(
                PlaceSearchRequest(
                    query = "멘야",
                    center = PlaceSearchCenterRequest(lat = 37.4, lng = 126.9),
                    mode = "locations",
                ),
                dataSource.request,
            )
            assertEquals(
                PlaceSearchResults(
                    listOf(
                        PlaceSearchResult(
                            name = "멘야 테스트",
                            address = "서울시 테스트로 1",
                            location = Location(lat = 37.5, lng = 127.0),
                            kind = PlaceSearchResultKind.MAP_LOCATION,
                        ),
                    ),
                ),
                result,
            )
        }

    @Test
    fun `결과 종류와 등록 매장 ID를 도메인 결과로 변환한다`() =
        runTest {
            val mapLocation =
                placeResponse(
                    name = "모란역",
                    kind = PlaceSearchResultKindResponse.MAP_LOCATION,
                )
            val registeredShop =
                placeResponse(
                    name = "멘야 테스트",
                    kind = PlaceSearchResultKindResponse.REGISTERED_SHOP,
                    shopId = "shop-1",
                )
            val unregisteredBusiness =
                placeResponse(
                    name = "테스트 치킨",
                    kind = PlaceSearchResultKindResponse.REGISTERED_SHOP,
                )
            val legacyUnclassifiedResult = placeResponse(name = "테스트 카페")
            val repository =
                DefaultPlaceSearchRepository(
                    FakePlaceSearchDataSource(
                        PlaceSearchResponse(
                            listOf(
                                mapLocation,
                                registeredShop,
                                unregisteredBusiness,
                                legacyUnclassifiedResult,
                            ),
                        ),
                    ),
                )

            val result = repository.search(SearchQuery("테스트"), Location(37.5, 127.0)).getOrThrow()

            assertEquals(
                listOf(
                    PlaceSearchResultKind.MAP_LOCATION to null,
                    PlaceSearchResultKind.REGISTERED_SHOP to "shop-1",
                    PlaceSearchResultKind.REGISTERED_SHOP to null,
                    PlaceSearchResultKind.UNCLASSIFIED to null,
                ),
                result.map { it.kind to it.shopId },
            )
        }
}

private fun placeResponse(
    name: String,
    kind: PlaceSearchResultKindResponse? = null,
    shopId: String? = null,
): PlaceSearchResultResponse =
    PlaceSearchResultResponse(
        name = name,
        address = "경기도 성남시 테스트로 1",
        lat = 37.5,
        lng = 127.0,
        kind = kind,
        shopId = shopId,
    )
