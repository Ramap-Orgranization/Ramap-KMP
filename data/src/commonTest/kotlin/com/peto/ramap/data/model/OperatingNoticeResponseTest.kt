package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class OperatingNoticeResponseTest {
    @Test
    fun `유효한 영업 변동을 도메인 모델로 변환한다`() {
        val notice = response().toDomain(ramenShopFixture())

        assertEquals(OperatingNoticeType.TEMPORARY_CLOSURE, notice.type)
        assertEquals("2026-08-20", notice.startDate.toString())
        assertEquals("2026-08-21", notice.endDate.toString())
        assertEquals("12:00", notice.endTime.toString())
    }

    @Test
    fun `종료일이 없는 영업 변동을 도메인 모델로 변환한다`() {
        val notice = response(endDate = null).toDomain(ramenShopFixture())

        assertEquals("2026-08-20", notice.startDate.toString())
        assertEquals(null, notice.endDate)
    }

    @Test
    fun `실제 영업 변동 유형을 도메인 유형으로 변환한다`() {
        val shop = ramenShopFixture()

        assertEquals(OperatingNoticeType.OPERATING_NOTICE, response("operating_notice").toDomain(shop).type)
        assertEquals(OperatingNoticeType.TEMPORARY_CLOSURE, response("full_close").toDomain(shop).type)
        assertEquals(OperatingNoticeType.EARLY_CLOSING, response("early_close").toDomain(shop).type)
    }

    @Test
    fun `잘못된 유형 날짜 또는 시간은 예외를 발생시킨다`() {
        val shop = ramenShopFixture()

        assertFails { response(noticeType = "unknown").toDomain(shop) }
        assertFails { response(startDate = "invalid-date").toDomain(shop) }
        assertFails { response(endDate = "invalid-date").toDomain(shop) }
        assertFails { response(endTime = "25:00").toDomain(shop) }
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
