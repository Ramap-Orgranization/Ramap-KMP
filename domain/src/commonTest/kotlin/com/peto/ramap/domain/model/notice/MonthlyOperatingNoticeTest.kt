package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.businesshour.BreakTime
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonthlyOperatingNoticeTest {
    private val regularDay = BusinessHoursDay(false, "11:00", "22:00", false, null)
    private val hours =
        BusinessHours(
            weekly = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun").associateWith { regularDay },
            breakTimes = mapOf("thu" to listOf(BreakTime("15:00", "17:00"))),
            lastOrders = emptyMap(),
            notice = null,
        )
    private val shop = ramenShopFixture().copy(businessHoursDetails = hours)

    @Test
    fun `떨어진 휴무일 사이의 날짜는 휴무로 처리하지 않는다`() {
        val notice = monthly(listOf(day("2026-10-06", true), day("2026-10-13", true)))
        assertEquals(BusinessHoursStatus.TemporaryClosure(LocalDate.parse("2026-10-06"), LocalDate.parse("2026-10-06")), shop.businessHoursStatus(at("2026-10-06T12:00"), listOf(notice)))
        assertTrue(shop.isOpenAt(at("2026-10-08T12:00"), listOf(notice)))
        assertEquals(BusinessHoursStatus.TemporaryClosure(LocalDate.parse("2026-10-13"), LocalDate.parse("2026-10-13")), shop.businessHoursStatus(at("2026-10-13T12:00"), listOf(notice)))
        assertTrue(shop.isOpenAt(at("2026-11-06T12:00"), listOf(notice)))
    }

    @Test
    fun `정상영업일은 기존 영업시간과 브레이크타임을 사용한다`() {
        val notice = monthly(listOf(day("2026-10-08", false)))
        assertTrue(shop.isOpenAt(at("2026-10-08T12:00"), listOf(notice)))
        assertEquals(BusinessHoursStatus.BreakTime("17:00"), shop.businessHoursStatus(at("2026-10-08T16:00"), listOf(notice)))
        assertFalse(shop.isOpenAt(at("2026-10-08T23:00"), listOf(notice)))
    }

    @Test
    fun `월간 공지의 표시 기간과 개별 적용 날짜는 구분한다`() {
        val notice = monthly(listOf(day("2026-10-06", true), day("2026-10-13", true)))
        assertFalse(notice.isActiveAt(at("2026-10-08T12:00")))
        assertTrue(notice.isCurrentOrScheduledAt(at("2026-10-08T12:00")))
        assertEquals(null, notice.scheduleOn(LocalDate.parse("2026-10-08"), hours))
        assertFalse(notice.isCurrentOrScheduledAt(at("2026-11-01T12:00")))
    }

    @Test
    fun `월말 공지를 새달 새벽 영업 상태 조회에서 유지한다`() {
        val overnightShop = shop.copy(businessHoursDetails = hours.copy(weekly = hours.weekly + ("sat" to regularDay.copy(open = "18:00", close = "02:00", closeNextDay = true))))
        val notice = monthly(listOf(day("2026-10-31", true))).copy(shop = overnightShop)
        assertTrue(notice.isRelevantForBusinessHoursAt(at("2026-11-01T01:00")))
        assertFalse(overnightShop.isOpenAt(at("2026-11-01T01:00"), listOf(notice)))
        assertEquals(BusinessHoursStatus.Closed("11:00"), overnightShop.businessHoursStatus(at("2026-11-01T01:00"), listOf(notice)))
        assertFalse(notice.isRelevantForBusinessHoursAt(at("2026-11-01T03:00")))
    }

    @Test
    fun `연속 휴무는 첫날과 마지막 날까지 동일한 기간으로 적용하고 다음 날 해제한다`() {
        val notice = monthly(emptyList()).copy(startDate = LocalDate.parse("2026-10-24"), endDate = LocalDate.parse("2026-10-30"))
        val expected = BusinessHoursStatus.TemporaryClosure(notice.startDate, notice.endDate)
        for (time in listOf("2026-10-24T00:00", "2026-10-27T16:00", "2026-10-30T23:59")) {
            assertEquals(expected, shop.businessHoursStatus(at(time), listOf(notice)))
            assertFalse(shop.isOpenAt(at(time), listOf(notice)))
        }
        assertTrue(shop.isOpenAt(at("2026-10-23T12:00"), listOf(notice)))
        assertTrue(shop.isOpenAt(at("2026-10-31T12:00"), listOf(notice)))
    }

    @Test
    fun `월간 공지에서는 오늘을 포함한 연속 휴무 구간만 반환한다`() {
        val notice = monthly(listOf(day("2026-10-06", true), day("2026-10-07", true), day("2026-10-13", true)))
        assertEquals(
            BusinessHoursStatus.TemporaryClosure(LocalDate.parse("2026-10-06"), LocalDate.parse("2026-10-07")),
            shop.businessHoursStatus(at("2026-10-07T12:00"), listOf(notice)),
        )
    }

    @Test
    fun `임시 휴무는 조기 마감보다 우선하며 다른 매장 공지는 적용하지 않는다`() {
        val notice = monthly(listOf(day("2026-10-06", true)))
        val earlyClosing = notice.copy(type = OperatingNoticeType.EARLY_CLOSING, endTime = kotlinx.datetime.LocalTime(15, 0))
        assertEquals(
            BusinessHoursStatus.TemporaryClosure(LocalDate.parse("2026-10-06"), LocalDate.parse("2026-10-06")),
            shop.businessHoursStatus(at("2026-10-06T16:00"), listOf(earlyClosing, notice)),
        )
        assertTrue(shop.isOpenAt(at("2026-10-06T12:00"), listOf(notice.copy(shop = shop.copy(id = "other")))))
    }

    private fun monthly(days: List<OperatingNoticeDay>) =
        OperatingNotice(
            id = "monthly",
            shop = shop,
            type = OperatingNoticeType.TEMPORARY_CLOSURE,
            description = "10월 운영 일정",
            startDate = LocalDate.parse("2026-10-01"),
            endDate = LocalDate.parse("2026-10-31"),
            startTime = null,
            endTime = null,
            sourceUrl = null,
            dailySchedules = days,
        )

    private fun day(
        date: String,
        closed: Boolean,
    ) = OperatingNoticeDay(LocalDate.parse(date), closed)

    private fun at(value: String) = LocalDateTime.parse(value)
}
