package com.peto.ramap.domain.model.place

import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceSearchResultsTest {
    @Test
    fun `판교면처럼 매장명과 겹치는 읍면 후보는 제외한다`() {
        val township = place("판교면", Location(36.1, 126.6)).copy(isAmbiguousRegion = true)
        val results = PlaceSearchResults(listOf(township))
        val shops = RamenShops(listOf(ramenShopFixture(name = "판교면학")))

        assertTrue(results.prioritizeShopNames(SearchQuery("판교면"), shops).isEmpty())
    }

    @Test
    fun `비토리처럼 매장명과 겹치는 동리 후보는 매장명 일치가 있으면 제외한다`() {
        val village = place("비토리", Location(34.97, 127.97)).copy(isAmbiguousRegion = true)
        val landmark = place("비토리공원", Location(37.44, 127.14))
        val results = PlaceSearchResults(listOf(village, landmark))
        val shops = RamenShops(listOf(ramenShopFixture(name = "멘야 비토리")))

        assertEquals(listOf(landmark), results.prioritizeShopNames(SearchQuery("비 토 리"), shops).toList())
        assertEquals(listOf(village, landmark), results.toList())
    }

    @Test
    fun `주소만 일치하거나 매장 결과가 없으면 동리 지역 후보를 유지한다`() {
        val village = place("비토리", Location(34.97, 127.97)).copy(isAmbiguousRegion = true)
        val results = PlaceSearchResults(listOf(village))
        val addressMatch = ramenShopFixture(name = "다른 라멘", address = "경상남도 사천시 서포면 비토리")

        assertEquals(results, results.prioritizeShopNames(SearchQuery("비토리"), RamenShops(listOf(addressMatch))))
        assertEquals(results, results.prioritizeShopNames(SearchQuery("비토리"), RamenShops(emptyList())))
    }

    @Test
    fun `세종 같은 광역 지역은 매장명이 일치해도 유지한다`() {
        val city = place("세종", Location(36.48, 127.29))
        val results = PlaceSearchResults(listOf(city))
        val shops = RamenShops(listOf(ramenShopFixture(name = "세종라멘")))

        assertEquals(results, results.prioritizeShopNames(SearchQuery("세종"), shops))
    }

    @Test
    fun `중심점과 가까운 장소부터 정렬한다`() {
        val farPlace = place(name = "먼 장소", location = Location(37.6, 127.1))
        val nearPlace = place(name = "가까운 장소", location = Location(37.5001, 127.0001))
        val results = PlaceSearchResults(listOf(farPlace, nearPlace))

        val sorted = results.nearestFirstTo(Location(37.5, 127.0))

        assertEquals(listOf(nearPlace, farPlace), sorted.toList())
        assertEquals(listOf(farPlace, nearPlace), results.toList())
    }

    @Test
    fun `거리가 같은 장소는 기존 순서와 중복을 유지한다`() {
        val location = Location(37.5, 127.0)
        val firstPlace = place(name = "첫 장소", location = location)
        val secondPlace = place(name = "두 번째 장소", location = location)
        val results = PlaceSearchResults(listOf(firstPlace, secondPlace, firstPlace))

        val sorted = results.nearestFirstTo(Location(37.4, 126.9))

        assertEquals(listOf(firstPlace, secondPlace, firstPlace), sorted.toList())
    }

    @Test
    fun `목록의 빈 값 개수 단일 원소 인덱스와 순회 의미를 유지한다`() {
        val place = place(name = "장소", location = Location(37.5, 127.0))
        val emptyResults = PlaceSearchResults(emptyList())
        val singleResults = PlaceSearchResults(listOf(place))

        assertTrue(emptyResults.isEmpty())
        assertEquals(1, singleResults.size)
        assertEquals(place, singleResults.single())
        assertEquals(place, singleResults[0])
        assertEquals(listOf(place), singleResults.map { it })
    }

    private fun place(
        name: String,
        location: Location,
    ): PlaceSearchResult =
        PlaceSearchResult(
            name = name,
            address = "테스트 주소",
            location = location,
        )
}
