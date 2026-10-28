package com.peto.ramap.designsystem.review

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewDateLabelTest {
    @Test
    fun `한국 시간 자정 이후 작성한 기록은 다음 날짜로 표시한다`() {
        assertEquals("2026.10.28", reviewDateLabel("2026-10-27T15:12:00Z", TimeZone.of("Asia/Seoul")))
    }

    @Test
    fun `표시하는 시간대에 따라 날짜를 변환한다`() {
        assertEquals("2026.10.27", reviewDateLabel("2026-10-28T00:12:00Z", TimeZone.of("America/Los_Angeles")))
        assertEquals("2026.10.28", reviewDateLabel("2026-10-28T00:12:00Z", TimeZone.UTC))
    }

    @Test
    fun `오프셋이 있는 타임스탬프도 시간대로 변환한다`() {
        assertEquals("2026.10.28", reviewDateLabel("2026-10-27T15:12:00+00:00", TimeZone.of("Asia/Seoul")))
    }

    @Test
    fun `시간 정보가 없거나 잘못된 날짜도 화면을 중단하지 않는다`() {
        assertEquals("2026.10.28", reviewDateLabel("2026-10-28", TimeZone.UTC))
        assertEquals("", reviewDateLabel("", TimeZone.UTC))
    }
}
