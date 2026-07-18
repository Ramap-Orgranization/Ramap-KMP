package com.peto.ramap.domain.model.place

import com.peto.ramap.domain.model.shop.Location
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceSearchResultsTest {
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
