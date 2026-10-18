package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class OperatingNoticeResponseTest {
    @Test
    fun `유효한 영업 변동을 도메인 모델로 변환한다`() {
        val notice = response().toDomain(ramenShopFixture())!!

        assertEquals(OperatingNoticeType.TEMPORARY_CLOSURE, notice.type)
        assertEquals("2026-08-20", notice.startDate.toString())
        assertEquals("2026-08-21", notice.endDate.toString())
        assertEquals("12:00", notice.endTime.toString())
    }

    @Test
    fun `종료일이 없는 영업 변동을 도메인 모델로 변환한다`() {
        val notice = response(endDate = null).toDomain(ramenShopFixture())!!

        assertEquals("2026-08-20", notice.startDate.toString())
        assertEquals(null, notice.endDate)
    }

    @Test
    fun `실제 영업 변동 유형을 도메인 유형으로 변환한다`() {
        val shop = ramenShopFixture()

        assertNull(response("operating_notice").toDomain(shop))
        assertNull(response("late_opening").toDomain(shop))
        assertEquals(OperatingNoticeType.TEMPORARY_CLOSURE, response("full_close").toDomain(shop)!!.type)
        assertEquals(OperatingNoticeType.EARLY_CLOSING, response("early_close").toDomain(shop)!!.type)
    }

    @Test
    fun `잘못된 유형 날짜 또는 시간은 예외를 발생시킨다`() {
        val shop = ramenShopFixture()

        assertNull(response(noticeType = "unknown").toDomain(shop))
        assertFails { response(startDate = "invalid-date").toDomain(shop) }
        assertFails { response(endDate = "invalid-date").toDomain(shop) }
        assertFails { response(endTime = "25:00").toDomain(shop) }
    }

    @Test
    fun `월간 일정의 날짜와 정상영업을 손실 없이 매핑한다`() {
        val notice =
            response(noticeType = "full_close")
                .copy(
                    dailySchedules =
                        listOf(
                            OperatingNoticeDayResponse("2026-08-21", false),
                            OperatingNoticeDayResponse("2026-08-20", true),
                        ),
                ).toDomain(ramenShopFixture())!!
        assertEquals(listOf("2026-08-20", "2026-08-21"), notice.dailySchedules.map { it.date.toString() })
        assertEquals(listOf(true, false), notice.dailySchedules.map { it.closed })
        assertEquals(null, notice.dailySchedules.last().scheduleOverride)
    }

    @Test
    fun `시각이 없는 기존 조기 마감 공지는 공개 모델에서 제외한다`() {
        assertNull(response(noticeType = "early_close", endTime = null).toDomain(ramenShopFixture()))
    }

    @Test
    fun `canonical 유형을 우선하고 모르는 canonical은 legacy로 강제하지 않는다`() {
        val legacy = response("early_close")
        assertEquals(OperatingNoticeType.PLANNED_CLOSING, legacy.copy(noticeCategory = "planned_close").toDomain(ramenShopFixture())!!.type)
        assertEquals(OperatingNoticeType.EARLY_CLOSING, legacy.toDomain(ramenShopFixture())!!.type)
        assertNull(legacy.copy(noticeCategory = "unknown").toDomain(ramenShopFixture()))
        assertNull(legacy.copy(noticeCategory = "planned_close", endTime = null).toDomain(ramenShopFixture()))
    }

    private fun response(
        noticeType: String = "temporary_closure",
        startDate: String = "2026-08-20",
        endDate: String? = "2026-08-21",
        endTime: String? = "12:00",
    ) = OperatingNoticeResponse(
        id = "notice",
        shopId = "shop",
        noticeType = noticeType,
        description = "내부 사정으로 쉽니다.",
        startDate = startDate,
        endDate = endDate,
        startTime = null,
        endTime = endTime,
        sourceUrl = null,
    )
}
