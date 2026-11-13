package com.peto.ramap.domain.model.shop

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopVisitTimeWindowTest {
    @Test
    fun `오늘부터 7일간 현재 시각 이후의 정각을 제공한다`() {
        val window = ShopVisitTimeWindow(LocalDateTime(2026, 11, 13, 18, 45))

        assertEquals((13..19).map { LocalDate(2026, 11, it) }, window.availableDates)
        assertEquals(LocalDateTime(2026, 11, 13, 19, 0), window.resolve(null).dateTime)
        assertEquals((19..23).map { LocalTime(it, 0) }, window.availableTimes(window.today))
        assertEquals((8..23).map { LocalTime(it, 0) }, window.availableTimes(window.tomorrow))
    }

    @Test
    fun `오전 8시 전에는 당일 8시부터 선택할 수 있다`() {
        val window = ShopVisitTimeWindow(LocalDateTime(2026, 11, 13, 6, 30))

        assertEquals(LocalDateTime(2026, 11, 13, 8, 0), window.resolve(null).dateTime)
        assertEquals(LocalTime(8, 0), window.availableTimes(window.today).first())
        assertEquals(LocalDateTime(2026, 11, 14, 8, 0), window.resolve(LocalDateTime(2026, 11, 14, 7, 0)).dateTime)
    }

    @Test
    fun `정각은 선택할 수 있고 시간이 지난 초안은 다음 선택 가능 시각으로 보정한다`() {
        val selected = LocalDateTime(2026, 11, 13, 19, 0)

        assertEquals(selected, ShopVisitTimeWindow(selected).resolve(selected).dateTime)
        assertEquals(
            LocalDateTime(2026, 11, 13, 20, 0),
            ShopVisitTimeWindow(LocalDateTime(2026, 11, 13, 19, 1)).resolve(selected).dateTime,
        )
    }

    @Test
    fun `오늘의 마지막 시간대가 지나면 다음 날 오전 8시부터 선택할 수 있다`() {
        val window = ShopVisitTimeWindow(LocalDateTime(2026, 11, 13, 23, 1))

        assertEquals((14..19).map { LocalDate(2026, 11, it) }, window.availableDates)
        assertEquals(emptyList(), window.availableTimes(window.today))
        assertEquals(LocalDateTime(2026, 11, 14, 8, 0), window.resolve(null).dateTime)
    }

    @Test
    fun `이전의 30분 단위 선택값을 선택 가능한 정각으로 보정한다`() {
        val window = ShopVisitTimeWindow(LocalDateTime(2026, 11, 13, 18, 45))

        assertEquals(LocalDateTime(2026, 11, 13, 20, 0), window.resolve(LocalDateTime(2026, 11, 13, 19, 30)).dateTime)
        assertEquals(LocalDateTime(2026, 11, 14, 8, 0), window.resolve(LocalDateTime(2026, 11, 13, 23, 30)).dateTime)
        assertEquals(LocalDateTime(2026, 11, 19, 23, 0), window.resolve(LocalDateTime(2026, 11, 19, 23, 30)).dateTime)
    }
}
