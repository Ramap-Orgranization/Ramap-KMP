package com.peto.ramap.domain.model.shop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ShopRegistrationMonthTest {
    @Test
    fun `서울의 자정부터 그달 등록으로 계산한다`() {
        val prior = ShopRegistrationMonth.containing(Instant.parse("2026-10-31T14:59:59.999Z"))
        val november = ShopRegistrationMonth.containing(Instant.parse("2026-10-31T15:00:00Z"))

        assertEquals(10, prior.monthNumber)
        assertEquals(11, november.monthNumber)
        assertEquals(Instant.parse("2026-10-31T15:00:00Z"), november.start)
        assertEquals(Instant.parse("2026-11-30T15:00:00Z"), november.endExclusive)
    }

    @Test
    fun `서울의 연말을 넘으면 다음 해 1월 범위를 적용한다`() {
        val december = ShopRegistrationMonth.containing(Instant.parse("2026-12-31T14:59:59Z"))
        val january = ShopRegistrationMonth.containing(Instant.parse("2026-12-31T15:00:00Z"))

        assertEquals(12, december.monthNumber)
        assertEquals(1, january.monthNumber)
        assertEquals(december.endExclusive, january.start)
        assertEquals(Instant.parse("2027-01-31T15:00:00Z"), january.endExclusive)
    }
}
