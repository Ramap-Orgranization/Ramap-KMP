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
    fun `ilike 패턴을 이스케이프하면 역슬래시 퍼센트 언더스코어를 일반 문자로 처리한다`() {
        // Given
        val query = SearchQuery("""라멘\맛집%_검색""")

        // When
        val actual = query.escapeIlikePattern()

        // Then
        assertEquals("""라멘\\맛집\%\_검색""", actual)
    }

    @Test
    fun `ilike 패턴을 이스케이프할 때 일반 문자는 변경하지 않는다`() {
        // Given
        val query = SearchQuery("요코다베이스")

        // When
        val actual = query.escapeIlikePattern()

        // Then
        assertEquals("요코다베이스", actual)
    }

    @Test
    fun `정규화한 검색어를 ilike 패턴용으로 이스케이프할 수 있다`() {
        // Given
        val query = SearchQuery("  RAMEN_%   SHOP  ").normalizeShopSearchQuery()

        // When
        val actual = query.escapeIlikePattern()

        // Then
        assertEquals("""ramen\_\% shop""", actual)
    }

    @Test
    fun `ilike 패턴은 앞뒤에 와일드카드를 추가한다`() {
        // Given
        val query = SearchQuery("ramen")

        // When
        val actual = query.ilikePattern()

        // Then
        assertEquals("%ramen%", actual)
    }

    @Test
    fun `ilike 패턴은 특수문자를 이스케이프한 뒤 와일드카드를 추가한다`() {
        // Given
        val query = SearchQuery("""라멘\맛집%_검색""")

        // When
        val actual = query.ilikePattern()

        // Then
        assertEquals("""%라멘\\맛집\%\_검색%""", actual)
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
        assertEquals("""%ramen\_\% shop%""", actual)
    }
}
