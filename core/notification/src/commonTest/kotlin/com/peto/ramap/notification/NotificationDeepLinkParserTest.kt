package com.peto.ramap.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationDeepLinkParserTest {
    private val parser = NotificationDeepLinkParser()
    private val eventId = "123e4567-e89b-12d3-a456-426614174000"

    @Test
    fun `기존 이벤트 링크와 버전 일 이벤트 명령을 함께 지원한다`() {
        assertEquals(NotificationDeepLink.Event(eventId), parser.parse("ramap://notification/event?event_id=$eventId"))
        assertEquals(NotificationDeepLink.Event(eventId), parser.parse("""{"version":1,"action":"event_detail","event_id":"$eventId"}"""))
    }

    @Test
    fun `기존 영업 변동 탭과 추가 이동 없음 명령을 읽는다`() {
        assertEquals(NotificationDeepLink.OperatingNotices, parser.parse("""{"version":1,"action":"operating_notices"}"""))
        assertEquals(NotificationDeepLink.None, parser.parse("""{"version":1,"action":"none"}"""))
    }

    @Test
    fun `공식 소셜과 매장 공개 도메인의 HTTPS 링크를 허용한다`() {
        for (url in listOf("https://www.instagram.com/p/example/", "https://ramen.example.com/notice?date=2026-11-08", "https://ramen.example.com:443/notice")) {
            assertEquals(NotificationDeepLink.ExternalUrl(url), parser.parse("""{"version":1,"action":"external_url","url":"$url"}"""))
        }
    }

    @Test
    fun `미지원 버전 동작과 모호한 이벤트 링크를 거절한다`() {
        for (value in listOf(null, "", "ramap://auth/callback", "ramap://notification/event?event_id=invalid", "ramap://notification/event?event_id=$eventId&event_id=$eventId", """{"version":2,"action":"none"}""", """{"version":"1","action":"none"}""", """{"version":1,"action":"unknown"}""", """{"version":1,"action":"none","url":"https://example.com"}""", "{")) {
            assertNull(parser.parse(value), value)
        }
    }

    @Test
    fun `실행 스킴 로컬 주소 사용자정보 포트와 잘못된 인코딩을 거절한다`() {
        val invalid = listOf("javascript:alert(1)", "intent://example", "file:///tmp/file", "tel:123", "http://example.com", "https://user@example.com/", "https://example.com:444/", "https://localhost/", "https://127.0.0.1/", "https://0x7f.0x0.0x0.0x1/", "https://router.lan/", "https://example.com/%ff", "https://example.com/%c0%af", "https://[::1]/", "https://ramen.internal/", "https://ramen.local/", "https://example.com./", "https://example.com/#fragment", "https://example.com/%00", "https://example.com/%5c", "https://example.com/%253a", "https://example.com/%GG", "https://example.com/%", "https://example.com/ space", "https://example.com/" + "a".repeat(2048))
        for (url in invalid) assertNull(parser.parse("""{"version":1,"action":"external_url","url":"$url"}"""), url)
    }
}
