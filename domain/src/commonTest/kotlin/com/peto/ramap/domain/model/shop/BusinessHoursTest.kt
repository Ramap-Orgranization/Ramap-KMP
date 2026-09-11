package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.businesshour.BreakTime
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.businesshour.BusinessHoursScheduleOverride
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessHoursTest {
    @Test
    fun `활성 영업 변동 공지는 영업중 필터에 반영된다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("sun" to BusinessHoursDay(false, "11:00", "22:00", false, null)),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val notice =
            OperatingNotice(
                id = "notice-1",
                shop = shop,
                type = OperatingNoticeType.TEMPORARY_CLOSURE,
                description = "임시 휴무",
                startDate = LocalDateTime(2026, 8, 30, 0, 0).date,
                endDate = LocalDateTime(2026, 8, 30, 0, 0).date,
                startTime = null,
                endTime = null,
                sourceUrl = null,
            )

        assertEquals(false, shop.isOpenAt(LocalDateTime(2026, 8, 30, 12, 0), listOf(notice)))
    }

    @Test
    fun `조기 마감과 늦은 오픈 공지는 공지 시간 기준으로 영업 여부를 제한한다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("sun" to BusinessHoursDay(false, "11:00", "22:00", false, null)),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val date = LocalDateTime(2026, 8, 30, 0, 0).date
        val earlyClosing =
            OperatingNotice(
                id = "early",
                shop = shop,
                type = OperatingNoticeType.EARLY_CLOSING,
                description = "조기 마감",
                startDate = date,
                endDate = date,
                startTime = null,
                endTime = LocalTime(15, 0),
                sourceUrl = null,
            )
        val lateOpening =
            earlyClosing.copy(
                id = "late",
                type = OperatingNoticeType.LATE_OPENING,
                startTime = LocalTime(15, 0),
                endTime = null,
            )

        assertEquals(true, shop.isOpenAt(LocalDateTime(2026, 8, 30, 14, 59), listOf(earlyClosing)))
        assertEquals(false, shop.isOpenAt(LocalDateTime(2026, 8, 30, 15, 0), listOf(earlyClosing)))
        assertEquals(false, shop.isOpenAt(LocalDateTime(2026, 8, 30, 14, 59), listOf(lateOpening)))
        assertEquals(true, shop.isOpenAt(LocalDateTime(2026, 8, 30, 15, 0), listOf(lateOpening)))
    }

    @Test
    fun `일정 변경과 미정 지연 오픈은 공통 상태 계산에 반영된다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("sun" to BusinessHoursDay(false, "11:00", "22:00", false, null)),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val date = LocalDateTime(2026, 8, 30, 0, 0).date
        val override = OperatingNotice("override", shop, OperatingNoticeType.OPERATING_NOTICE, "변경", date, date, null, null, BusinessHoursScheduleOverride(BusinessHoursDay(false, "15:00", "20:00", false, null)), null, null)
        val delayed = OperatingNotice("late", shop, OperatingNoticeType.LATE_OPENING, "지연", date, date, null, null, null, null, null)

        assertEquals(false, shop.isOpenAt(LocalDateTime(2026, 8, 30, 14, 0), listOf(override)))
        assertEquals(true, shop.isOpenAt(LocalDateTime(2026, 8, 30, 16, 0), listOf(override)))
        assertEquals(false, shop.isOpenAt(LocalDateTime(2026, 8, 30, 16, 0), listOf(delayed)))
    }

    @Test
    fun `같은 날짜 중복 일정 변경은 가장 최근 수정본을 적용한다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("sun" to BusinessHoursDay(false, "11:00", "22:00", false, null)),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val date = LocalDateTime(2026, 8, 30, 0, 0).date
        val old =
            OperatingNotice(
                id = "old",
                shop = shop,
                type = OperatingNoticeType.OPERATING_NOTICE,
                description = "이전",
                startDate = date,
                endDate = date,
                startTime = null,
                endTime = null,
                scheduleOverride = BusinessHoursScheduleOverride(BusinessHoursDay(false, "12:00", "18:00", false, null)),
                manuallyReleasedAt = null,
                sourceUrl = null,
                updatedAt = "2026-08-29T01:00:00Z",
            )
        val latest =
            old.copy(
                id = "latest",
                description = "최신",
                scheduleOverride = BusinessHoursScheduleOverride(BusinessHoursDay(false, "15:00", "20:00", false, null)),
                updatedAt = "2026-08-30T01:00:00Z",
            )

        assertEquals(false, shop.isOpenAt(LocalDateTime(2026, 8, 30, 13, 0), listOf(latest, old)))
        assertEquals(true, shop.isOpenAt(LocalDateTime(2026, 8, 30, 16, 0), listOf(old, latest)))
        assertEquals("최신", shop.latestScheduleOverride(LocalDateTime(2026, 8, 30, 16, 0), listOf(old, latest))?.description)
    }

    @Test
    fun `익일 마감 공지는 영업일 기준으로 조기 마감과 지연 오픈을 계산한다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("sat" to BusinessHoursDay(false, "18:00", "02:00", true, null)),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val saturday = LocalDateTime(2026, 8, 29, 0, 0).date
        val early = OperatingNotice("early", shop, OperatingNoticeType.EARLY_CLOSING, "조기", saturday, saturday, null, LocalTime(23, 0), null, null, null)
        val delayed = OperatingNotice("late", shop, OperatingNoticeType.LATE_OPENING, "지연", saturday, saturday, LocalTime(20, 0), null, null, null, null)

        assertEquals(false, shop.isOpenAt(LocalDateTime(2026, 8, 30, 1, 0), listOf(early)))
        assertEquals(true, shop.isOpenAt(LocalDateTime(2026, 8, 30, 1, 0), listOf(delayed)))
    }

    @Test
    fun `전날 당일 영업시간은 다음 날 영업중으로 판정하지 않는다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly =
                            mapOf(
                                "sat" to BusinessHoursDay(false, "11:30", "21:00", false, null),
                                "sun" to BusinessHoursDay(true, null, null, false, "정기휴무"),
                            ),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )

        assertEquals(false, shop.isOpenAt(LocalDateTime(2026, 8, 30, 20, 32)))
    }

    @Test
    fun `영업시간 데이터의 의미 필드를 보존한다`() {
        val hours =
            BusinessHours(
                weekly = mapOf("sat" to BusinessHoursDay(false, "11:00", "16:00", false, null)),
                breakTimes = mapOf("sat" to listOf(BreakTime("13:00", "14:00"))),
                lastOrders = mapOf("sat" to listOf("15:30")),
                notice = "마지막 주문 마감 30분전",
            )

        assertEquals("11:00", hours.weekly.getValue("sat").open)
        assertEquals(
            "14:00",
            hours
                .breakTimes
                .getValue("sat")
                .single()
                .end,
        )
        assertEquals(listOf("15:30"), hours.lastOrders.getValue("sat"))
        assertEquals("마지막 주문 마감 30분전", hours.notice)
    }

    @Test
    fun `매장 영업 상태와 다음 영업 시간을 계산한다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly =
                            mapOf(
                                "mon" to BusinessHoursDay(false, "11:00", "22:00", false, null),
                                "tue" to BusinessHoursDay(false, "12:00", "21:00", false, null),
                            ),
                        breakTimes =
                            mapOf(
                                "mon" to
                                    listOf(
                                        BreakTime(
                                            "14:00",
                                            "15:00",
                                        ),
                                    ),
                            ),
                        lastOrders = mapOf("mon" to listOf("13:30", "21:00")),
                        notice = null,
                    ),
            )

        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("13:30"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 10, 13, 0)),
        )
        assertEquals(
            BusinessHoursStatus.BreakTime("15:00"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 10, 14, 30)),
        )
        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("21:00"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 10, 16, 0)),
        )
        assertEquals(
            BusinessHoursStatus.Closed("12:00"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 10, 23, 0)),
        )
    }

    @Test
    fun `영업 변동으로 닫힌 매장은 다음 영업시간 없이 영업 종료 상태를 표시한다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("sun" to BusinessHoursDay(false, "11:00", "22:00", false, null)),
                        breakTimes = mapOf("sun" to listOf(BreakTime("13:00", "14:00"))),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val date = LocalDateTime(2026, 8, 30, 0, 0).date
        val temporaryClosure =
            OperatingNotice(
                id = "temporary",
                shop = shop,
                type = OperatingNoticeType.TEMPORARY_CLOSURE,
                description = "임시 휴무",
                startDate = date,
                endDate = date,
                startTime = null,
                endTime = null,
                sourceUrl = null,
            )

        assertEquals(
            BusinessHoursStatus.Closed(),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 30, 12, 0), listOf(temporaryClosure)),
        )
        assertEquals(
            BusinessHoursStatus.Closed(),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 30, 13, 30), listOf(temporaryClosure)),
        )

        val earlyClosing =
            temporaryClosure.copy(
                id = "early",
                type = OperatingNoticeType.EARLY_CLOSING,
                endTime = LocalTime(15, 0),
            )
        val lateOpening =
            temporaryClosure.copy(
                id = "late",
                type = OperatingNoticeType.LATE_OPENING,
                startTime = LocalTime(15, 0),
            )

        assertEquals(
            BusinessHoursStatus.Closed(),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 30, 15, 0), listOf(earlyClosing)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("22:00"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 30, 14, 59), listOf(earlyClosing)),
        )
        assertEquals(
            BusinessHoursStatus.Closed(),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 30, 14, 59), listOf(lateOpening)),
        )
        assertEquals(
            BusinessHoursStatus.OpenUntil("22:00"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 30, 15, 0), listOf(lateOpening)),
        )
    }
}
