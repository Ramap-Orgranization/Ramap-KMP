package com.peto.ramap.domain.model.announcement

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class AnnouncementTest {
    @Test
    fun `공지사항의 날짜를 지정된 구분자로 포맷팅한다`() {
        val announcement =
            Announcement(
                id = "1",
                title = "공지사항 제목",
                bodyMarkdown = "본문 내용",
                publishedAt = Instant.parse("2026-11-01T15:30:00Z"),
            )

        val dotFormatted = announcement.formattedPublishedDate(delimiter = ".", timeZone = TimeZone.of("UTC"))
        val dashFormatted = announcement.formattedPublishedDate(delimiter = "-", timeZone = TimeZone.of("UTC"))

        assertEquals("2026.11.01", dotFormatted)
        assertEquals("2026-11-01", dashFormatted)
    }

    @Test
    fun `읽음 상태 목록에 아이디가 존재하지 않으면 안읽음으로 판단한다`() {
        val announcement =
            Announcement(
                id = "1",
                title = "공지사항",
                bodyMarkdown = "내용",
                publishedAt = Instant.parse("2026-11-01T00:00:00Z"),
            )

        assertTrue(announcement.isUnread(setOf("2", "3")))
        assertFalse(announcement.isUnread(setOf("1", "2")))
    }
}
