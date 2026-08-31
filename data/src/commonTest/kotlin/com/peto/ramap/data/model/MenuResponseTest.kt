package com.peto.ramap.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MenuResponseTest {
    @Test
    fun `visible menu 응답의 is_featured를 대표 메뉴로 역직렬화한다`() {
        val response =
            Json.decodeFromString<MenuResponse>(
                """{"id":"menu","section_id":"section","name":"시오라멘","price_krw":10000,"display_order":0,"is_featured":true}""",
            )

        assertEquals(true, response.isRepresentative)
    }

    @Test
    fun `visible menu 응답의 원문 링크를 역직렬화한다`() {
        val response =
            Json.decodeFromString<MenuResponse>(
                """{"id":"menu","section_id":"section","name":"네이버 소식 / 인스타그램 참고","price_krw":null,"source_url":"https://pcmap.place.naver.com/restaurant/2030397790/feed","display_order":0,"is_featured":true}""",
            )

        assertEquals("https://pcmap.place.naver.com/restaurant/2030397790/feed", response.sourceUrl)
    }
}
