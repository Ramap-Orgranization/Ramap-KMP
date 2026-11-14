package com.peto.ramap.domain.model.shop

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ShopOpeningMonthTest {
    @Test
    fun `서울의 자정을 기준으로 오픈일을 조회할 달을 계산한다`() {
        val prior = ShopOpeningMonth.containing(Instant.parse("2026-10-31T14:59:59.999Z"))
        val november = ShopOpeningMonth.containing(Instant.parse("2026-10-31T15:00:00Z"))

        assertEquals(10, prior.monthNumber)
        assertEquals(11, november.monthNumber)
        assertEquals(LocalDate(2026, 10, 1), prior.firstDay)
        assertEquals(LocalDate(2026, 11, 1), november.firstDay)
        assertEquals(LocalDate(2026, 12, 1), november.endExclusive)
    }

    @Test
    fun `서울의 연말을 넘으면 다음 해 1월을 적용한다`() {
        val december = ShopOpeningMonth.containing(Instant.parse("2026-12-31T14:59:59Z"))
        val january = ShopOpeningMonth.containing(Instant.parse("2026-12-31T15:00:00Z"))

        assertEquals(12, december.monthNumber)
        assertEquals(1, january.monthNumber)
        assertEquals(LocalDate(2026, 12, 1), december.firstDay)
        assertEquals(december.endExclusive, january.firstDay)
        assertEquals(LocalDate(2027, 2, 1), january.endExclusive)
    }
}
