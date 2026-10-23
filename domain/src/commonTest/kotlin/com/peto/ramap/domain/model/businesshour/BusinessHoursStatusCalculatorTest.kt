package com.peto.ramap.domain.model.businesshour

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessHoursStatusCalculatorTest {
    private val hours =
        BusinessHours(
            weekly = mapOf("mon" to BusinessHoursDay(false, "11:00", "19:30", false, null)),
            breakTimes = mapOf("mon" to listOf(BreakTime("15:00", "17:00"))),
            lastOrders = mapOf("mon" to listOf("14:30")),
            notice = null,
        )

    @Test
    fun `오전 라스트오더가 지나면 마감 시각을 표시하고 오후에도 재사용하지 않는다`() {
        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("14:30"),
            hours.statusAt(LocalDateTime(2026, 8, 10, 14, 29)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("19:30"),
            hours.statusAt(LocalDateTime(2026, 8, 10, 14, 30)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("19:30"),
            hours.statusAt(LocalDateTime(2026, 8, 10, 14, 31)),
        )
        assertEquals(
            BusinessHoursStatus.BreakTime("17:00"),
            hours.statusAt(LocalDateTime(2026, 8, 10, 15, 0)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("19:30"),
            hours.statusAt(LocalDateTime(2026, 8, 10, 17, 0)),
        )
        assertEquals(
            BusinessHoursStatus.Closed("11:00"),
            hours.statusAt(LocalDateTime(2026, 8, 10, 19, 30)),
        )
    }

    @Test
    fun `오후 라스트오더만 등록되어 있으면 오후 영업에만 표시한다`() {
        val afternoonHours = hours.copy(lastOrders = mapOf("mon" to listOf("19:00")))

        assertEquals(
            BusinessHoursStatus.OpenUntil("19:30"),
            afternoonHours.statusAt(LocalDateTime(2026, 8, 10, 13, 0)),
        )
        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("19:00"),
            afternoonHours.statusAt(LocalDateTime(2026, 8, 10, 17, 0)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("19:30"),
            afternoonHours.statusAt(LocalDateTime(2026, 8, 10, 19, 1)),
        )
    }

    @Test
    fun `등록 순서와 관계없이 현재 영업 구간의 다음 라스트오더를 표시한다`() {
        val unorderedHours = hours.copy(lastOrders = mapOf("mon" to listOf("19:00", "14:30")))

        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("14:30"),
            unorderedHours.statusAt(LocalDateTime(2026, 8, 10, 13, 0)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("19:30"),
            unorderedHours.statusAt(LocalDateTime(2026, 8, 10, 14, 31)),
        )
        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("19:00"),
            unorderedHours.statusAt(LocalDateTime(2026, 8, 10, 18, 0)),
        )
    }

    @Test
    fun `익일 라스트오더는 자정 전후에 표시하고 지나면 마감 시각을 표시한다`() {
        val overnightHours =
            hours.copy(
                weekly = mapOf("mon" to BusinessHoursDay(false, "18:00", "02:00", true, null)),
                breakTimes = emptyMap(),
                lastOrders = mapOf("mon" to listOf("01:30")),
            )

        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("01:30"),
            overnightHours.statusAt(LocalDateTime(2026, 8, 10, 23, 0)),
        )
        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("01:30"),
            overnightHours.statusAt(LocalDateTime(2026, 8, 11, 1, 0)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("02:00"),
            overnightHours.statusAt(LocalDateTime(2026, 8, 11, 1, 31)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("02:00"),
            overnightHours
                .copy(lastOrders = mapOf("mon" to listOf("23:30")))
                .statusAt(LocalDateTime(2026, 8, 11, 1, 0)),
        )
    }

    @Test
    fun `자정을 넘기는 휴게시간 이후에는 이전 영업 구간의 라스트오더를 표시하지 않는다`() {
        val overnightHours =
            hours.copy(
                weekly = mapOf("mon" to BusinessHoursDay(false, "18:00", "02:00", true, null)),
                breakTimes = mapOf("mon" to listOf(BreakTime("23:00", "00:30"))),
                lastOrders = mapOf("mon" to listOf("22:30", "01:30")),
            )

        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("22:30"),
            overnightHours.statusAt(LocalDateTime(2026, 8, 10, 22, 0)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("02:00"),
            overnightHours.statusAt(LocalDateTime(2026, 8, 10, 22, 31)),
        )
        assertEquals(
            BusinessHoursStatus.BreakTime("00:30"),
            overnightHours.statusAt(LocalDateTime(2026, 8, 11, 0, 0)),
        )
        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("01:30"),
            overnightHours.statusAt(LocalDateTime(2026, 8, 11, 0, 30)),
        )
    }
}
