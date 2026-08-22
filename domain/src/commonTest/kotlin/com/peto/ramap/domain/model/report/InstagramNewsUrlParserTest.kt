package com.peto.ramap.domain.model.report

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstagramNewsUrlParserTest {
    @Test
    fun `공유 내용의 인스타그램 게시물 링크를 표준 URL로 추출한다`() {
        assertEquals(
            "https://www.instagram.com/p/ABC123",
            InstagramNewsUrlParser.extractCanonicalUrl("새 메뉴 https://instagram.com/p/ABC123/?utm_source=share"),
        )
        assertEquals(
            "https://www.instagram.com/reel/XYZ789",
            InstagramNewsUrlParser.extractCanonicalUrl("https://www.instagram.com/reel/XYZ789#share"),
        )
        assertNull(InstagramNewsUrlParser.extractCanonicalUrl("https://instagram.com/ramap_official"))
        assertNull(InstagramNewsUrlParser.extractCanonicalUrl("https://www.instagram.com/p/"))
    }
}
