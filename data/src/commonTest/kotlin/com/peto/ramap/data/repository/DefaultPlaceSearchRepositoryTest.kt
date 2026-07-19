package com.peto.ramap.data.repository

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.PlaceSearchCenterRequest
import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.data.model.PlaceSearchResponse
import com.peto.ramap.data.model.PlaceSearchResultResponse
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.fake.FakePlaceSearchDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultPlaceSearchRepositoryTest {
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
                        ),
                    ),
                ),
                result,
            )
        }
}
