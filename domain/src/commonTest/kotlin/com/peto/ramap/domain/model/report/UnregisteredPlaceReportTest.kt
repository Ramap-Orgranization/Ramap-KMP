package com.peto.ramap.domain.model.report

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnregisteredPlaceReportTest {
    @Test
    fun `카카오맵 공유 내용에서 장소 링크를 추출한다`() {
        val content =
            """[카카오맵] 신멘
            |경기 안양시 동안구 호성로 20
            |https://kko.to/hgONCY9DKH
            """.trimMargin()

        assertEquals("https://kko.to/hgONCY9DKH", PlaceReportTextParser.extractSupportedUrl(content))
    }

    @Test
    fun `네이버 지도 공유 내용에서 장소 링크를 추출한다`() {
        val content =
            """[네이버지도] 이리에라멘
            |서울 마포구 성지1길 18 1층
            |https://naver.me/GahpmIBD
            """.trimMargin()

        assertEquals("https://naver.me/GahpmIBD", PlaceReportTextParser.extractSupportedUrl(content))
    }

    @Test
    fun `지원하지 않는 링크만 있으면 추출하지 않는다`() {
        assertNull(PlaceReportTextParser.extractSupportedUrl("https://example.com/place"))
    }

    @Test
    fun `공유 내용의 상호명이나 주소가 기존 매장과 같으면 일치한다`() {
        val shop =
            com.peto.ramap.fixture
                .ramenShopFixture(name = "신멘", address = "경기 안양시 동안구 호성로 20")
        val content =
            """[카카오맵] 신멘
            |경기 안양시 동안구 호성로 20
            |https://kko.to/example
            """.trimMargin()

        assertTrue(PlaceReportTextParser.matchesSharedPlace(content, shop))
    }

    @Test
    fun `해석된 카카오 장소 ID가 기존 매장과 같으면 일치한다`() {
        val shop =
            com.peto.ramap.fixture
                .ramenShopFixture()
                .copy(kakaoPlaceUrl = "https://place.map.kakao.com/1521564391")

        assertTrue(
            PlaceReportTextParser.matchesResolvedPlace(
                ResolvedPlaceLink(PlaceLinkProvider.KAKAO, placeId = "1521564391"),
                shop,
            ),
        )
    }

    @Test
    fun `카카오 장소 URL에서 ID를 추출한다`() {
        assertEquals(
            "1521564391",
            com.peto.ramap.domain.model.shop.KakaoPlaceUrl.extractPlaceId(
                "https://place.map.kakao.com/1521564391?referrer=share",
            ),
        )
        assertNull(com.peto.ramap.domain.model.shop.KakaoPlaceUrl.extractPlaceId("https://kko.to/example"))
    }
}
