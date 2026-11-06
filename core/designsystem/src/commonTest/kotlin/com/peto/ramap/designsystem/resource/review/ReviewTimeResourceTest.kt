package com.peto.ramap.designsystem.resource.review

import com.peto.ramap.designsystem.resource.UiText
import kotlinx.datetime.TimeZone
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_time_date
import ramap.shared.generated.resources.review_time_days_ago
import ramap.shared.generated.resources.review_time_hours_ago
import ramap.shared.generated.resources.review_time_just_now
import ramap.shared.generated.resources.review_time_minutes_ago
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ReviewTimeResourceTest {
    private val writtenAt = Instant.parse("2026-10-27T15:12:00Z")
    private val timeZone = TimeZone.of("Asia/Seoul")

    @Test
    fun `1분 미만과 미래 작성 시각은 방금 전으로 표시한다`() {
        val justNow = UiText(Res.string.review_time_just_now)
        assertEquals(justNow, labelAfter(59.seconds))
        assertEquals(justNow, labelAfter((-1).seconds))
    }

    @Test
    fun `1분부터 1시간 미만까지는 지난 분을 표시한다`() {
        assertEquals(UiText(Res.string.review_time_minutes_ago, listOf(1L)), labelAfter(1.minutes))
        assertEquals(UiText(Res.string.review_time_minutes_ago, listOf(59L)), labelAfter(1.hours - 1.seconds))
    }

    @Test
    fun `1시간부터 24시간 미만까지는 지난 시간을 표시한다`() {
        assertEquals(UiText(Res.string.review_time_hours_ago, listOf(1L)), labelAfter(1.hours))
        assertEquals(UiText(Res.string.review_time_hours_ago, listOf(23L)), labelAfter(1.days - 1.seconds))
    }

    @Test
    fun `24시간부터 7일 미만까지는 지난 일수를 표시한다`() {
        assertEquals(UiText(Res.string.review_time_days_ago, listOf(1L)), labelAfter(1.days))
        assertEquals(UiText(Res.string.review_time_days_ago, listOf(6L)), labelAfter(7.days - 1.seconds))
    }

    @Test
    fun `7일이 지나면 기기 시간대의 작성일을 표시한다`() {
        val date = UiText(Res.string.review_time_date, listOf("2026.10.28"))
        assertEquals(date, labelAfter(7.days))
        assertEquals(date, labelAfter(400.days))
    }

    @Test
    fun `오프셋이 있는 작성 시각은 같은 순간으로 해석한다`() {
        assertEquals(
            Instant.parse("2026-10-27T15:12:00Z"),
            reviewCreationTime("2026-10-28T00:12:00+09:00", TimeZone.UTC),
        )
    }

    @Test
    fun `날짜만 있는 기록은 기기 시간대의 날짜 시작을 사용한다`() {
        assertEquals(
            Instant.parse("2026-10-27T15:00:00Z"),
            reviewCreationTime("2026-10-28", TimeZone.of("Asia/Seoul")),
        )
    }

    @Test
    fun `작성 시각이 비어 있거나 올바르지 않으면 생략한다`() {
        assertNull(reviewCreationTime("", TimeZone.UTC))
        assertNull(reviewCreationTime("2026-02-30", TimeZone.UTC))
    }

    private fun labelAfter(elapsed: Duration): UiText =
        reviewTimeResource(
            writtenAt = writtenAt,
            now = writtenAt + elapsed,
            timeZone = timeZone,
        )
}
