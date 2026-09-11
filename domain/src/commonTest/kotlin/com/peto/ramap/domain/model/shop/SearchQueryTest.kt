package com.peto.ramap.domain.model.shop

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchQueryTest {
    @Test
    fun `검색어를 정규화하면 앞뒤 공백을 제거하고 연속 공백을 합친 뒤 소문자로 변환한다`() {
        // Given
        val query = SearchQuery("  Ichiran   RAMEN  강남\t점  ")

        // When
        val actual = query.normalizeShopSearchQuery()

        // Then
        assertEquals(SearchQuery("ichiran ramen 강남 점"), actual)
    }

    @Test
    fun `공백만 있는 검색어를 정규화하면 빈 문자열을 반환한다`() {
        // Given
        val query = SearchQuery(" \n\t ")

        // When
        val actual = query.normalizeShopSearchQuery()

        // Then
        assertEquals(SearchQuery(""), actual)
    }

    @Test
    fun `ilike 패턴은 입력과 저장된 상호의 공백을 무시한다`() {
        // Given
        val spacedQuery = SearchQuery("멘야 타마시")
        val unspacedQuery = SearchQuery("멘야타마시")

        // When
        val spacedPattern = spacedQuery.ilikePattern()
        val unspacedPattern = unspacedQuery.ilikePattern()

        // Then
        assertEquals("%멘%야%타%마%시%", spacedPattern)
        assertEquals(spacedPattern, unspacedPattern)
    }

    @Test
    fun `ilike 패턴은 특수문자를 이스케이프한 뒤 와일드카드를 추가한다`() {
        // Given
        val query = SearchQuery("""라멘\맛집%_검색""")

        // When
        val actual = query.ilikePattern()

        // Then
        assertEquals("""%라%멘%\\%맛%집%\%%\_%검%색%""", actual)
    }

    @Test
    fun `정규화된 검색어를 ilike 패턴으로 변환할 수 있다`() {
        // Given
        val query =
            SearchQuery("  RAMEN_%   SHOP  ")
                .normalizeShopSearchQuery()

        // When
        val actual = query.ilikePattern()

        // Then
        assertEquals("""%r%a%m%e%n%\_%\%%s%h%o%p%""", actual)
    }
}
