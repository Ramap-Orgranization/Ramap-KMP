package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.businesshour.BreakTime
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EarlyClosingNoticeTest {
    private val hours =
        BusinessHours(
            weekly = mapOf("wed" to BusinessHoursDay(false, "11:00", "19:30", false, null)),
            breakTimes = mapOf("wed" to listOf(BreakTime("15:00", "17:00"))),
            lastOrders = mapOf("wed" to listOf("14:30", "19:00")),
            notice = null,
        )
    private val shop = ramenShopFixture().copy(businessHoursDetails = hours)
    private val notice =
        OperatingNotice(
            id = "early",
            shop = shop,
            type = OperatingNoticeType.EARLY_CLOSING,
            description = "점심만 영업",
            startDate = LocalDate(2026, 10, 14),
            endDate = LocalDate(2026, 10, 14),
            startTime = null,
            endTime = LocalTime(15, 0),
            sourceUrl = null,
        )

    @Test
    fun `운영 변동은 정규 영업과 휴게를 유지하고 실제 조기 마감과 가장 이른 시각을 적용한다`() {
        val planned = notice.copy(type = OperatingNoticeType.PLANNED_CLOSING, endTime = LocalTime(18, 0))
        assertEquals(BusinessHoursStatus.BreakTime("17:00"), shop.businessHoursStatus(LocalDateTime(2026, 10, 14, 16, 0), listOf(planned)))
        assertEquals(BusinessHoursStatus.OpenUntil("18:00"), shop.businessHoursStatus(LocalDateTime(2026, 10, 14, 17, 59), listOf(planned)))
        assertEquals(BusinessHoursStatus.Closed(), shop.businessHoursStatus(LocalDateTime(2026, 10, 14, 18, 0), listOf(planned)))
        assertEquals(BusinessHoursStatus.Closed(), shop.businessHoursStatus(LocalDateTime(2026, 10, 14, 15, 0), listOf(planned, notice)))
        val closed = notice.copy(type = OperatingNoticeType.TEMPORARY_CLOSURE, endTime = null)
        assertFalse(shop.isOpenAt(LocalDateTime(2026, 10, 14, 12, 0), listOf(planned, notice, closed)))
        assertEquals(LocalDateTime(2026, 10, 14, 18, 0), planned.nextClosingAt(LocalDateTime(2026, 10, 14, 17, 0)))
    }

    @Test
    fun `점심 조기 마감 직전에는 변경된 마감 시각을 안내하고 영업중을 유지한다`() {
        val now = LocalDateTime(2026, 10, 14, 14, 59)
        assertEquals(BusinessHoursStatus.OpenUntil("15:00"), shop.businessHoursStatus(now, listOf(notice)))
        assertTrue(shop.isOpenAt(now, listOf(notice)))
    }

    @Test
    fun `확인된 마감 시각부터 저녁 재개 시간에도 영업 종료를 유지한다`() {
        for (hour in listOf(15, 17, 19)) {
            val now = LocalDateTime(2026, 10, 14, hour, 0)
            assertEquals(BusinessHoursStatus.Closed(), shop.businessHoursStatus(now, listOf(notice)))
            assertFalse(shop.isOpenAt(now, listOf(notice)))
        }
    }

    @Test
    fun `마감 전 휴게시간을 유지하고 휴게시간 뒤에는 조기 마감 시각을 안내한다`() {
        val later = notice.copy(endTime = LocalTime(18, 0))
        assertEquals(BusinessHoursStatus.BreakTime("17:00"), shop.businessHoursStatus(LocalDateTime(2026, 10, 14, 16, 0), listOf(later)))
        assertEquals(BusinessHoursStatus.OpenUntil("18:00"), shop.businessHoursStatus(LocalDateTime(2026, 10, 14, 17, 0), listOf(later)))
    }

    @Test
    fun `상태 갱신은 미래의 정확한 조기 마감 시각을 사용하고 지난 시각은 제외한다`() {
        assertEquals(LocalDateTime(2026, 10, 14, 15, 0), notice.nextClosingAt(LocalDateTime(2026, 10, 14, 14, 59, 45)))
        assertEquals(null, notice.nextClosingAt(LocalDateTime(2026, 10, 14, 15, 0)))
    }

    @Test
    fun `다른 매장의 조기 마감이나 미래 공지는 오늘 상태를 바꾸지 않는다`() {
        val now = LocalDateTime(2026, 10, 14, 18, 0)
        assertTrue(shop.isOpenAt(now, listOf(notice.copy(shop = shop.copy(id = "other")))))
        assertTrue(shop.isOpenAt(now, listOf(notice.copy(startDate = LocalDate(2026, 10, 15), endDate = LocalDate(2026, 10, 15)))))
    }

    @Test
    fun `조기 마감 공지가 정규 마감보다 늦어도 영업시간을 연장하지 않는다`() {
        assertEquals(BusinessHoursStatus.OpenWithLastOrder("19:00"), shop.businessHoursStatus(LocalDateTime(2026, 10, 14, 18, 0), listOf(notice.copy(endTime = LocalTime(21, 0)))))
        assertFalse(shop.isOpenAt(LocalDateTime(2026, 10, 14, 20, 0), listOf(notice.copy(endTime = LocalTime(21, 0)))))
    }

    @Test
    fun `익일 새벽 조기 마감 공지는 전날 공지 조회에서 제외되지 않는다`() {
        val overnight = shop.copy(businessHoursDetails = hours.copy(weekly = mapOf("wed" to BusinessHoursDay(false, "18:00", "02:00", true, null)), breakTimes = emptyMap(), lastOrders = emptyMap()))
        val overnightNotice = notice.copy(shop = overnight, endTime = LocalTime(1, 0))
        val before = LocalDateTime(2026, 10, 15, 0, 59)
        assertTrue(overnightNotice.isRelevantForBusinessHoursAt(before))
        assertEquals(BusinessHoursStatus.OpenUntil("01:00"), overnight.businessHoursStatus(before, listOf(overnightNotice)))
        assertEquals(BusinessHoursStatus.Closed(), overnight.businessHoursStatus(LocalDateTime(2026, 10, 15, 1, 0), listOf(overnightNotice)))
    }
}
