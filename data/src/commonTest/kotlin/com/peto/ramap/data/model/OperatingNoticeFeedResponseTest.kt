package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AreaFilter
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OperatingNoticeFeedResponseTest {
    @Test
    fun `주소가 없는 목록 응답도 공지와 지역 필터 정보를 유지한다`() {
        val response =
            Json.decodeFromString<OperatingNoticeFeedResponse>(
                """
                {
                  "reference_date": "2026-10-18",
                  "items": [{
                    "notice": {
                      "id": "notice", "shop_id": "shop", "notice_type": "early_close",
                      "notice_category": "planned_close", "description": "단축 영업",
                      "notice_date": "2026-10-18", "end_time": "15:45:00",
                      "end_date": null, "start_time": null, "source_url": null
                    },
                    "shop": {
                      "id": "shop", "name": "라멘", "lat": 37.0, "lng": 127.0,
                      "created_at": "", "updated_at": "",
                      "business_hours_weekly": {"sun": {"open": "11:00", "close": "20:00", "closed": false}}
                    },
                    "area": "GYEONGGI", "sigungu": "평택시"
                  }]
                }
                """.trimIndent(),
            )
        val feed = response.toDomain()
        val notice =
            feed
                .representatives(
                    scheduled = false,
                    areaFilter = AreaFilter.District(AdministrativeArea.GYEONGGI, AdministrativeDistrict("평택시")),
                ).single()

        assertEquals("", notice.shop.address)
        assertEquals("라멘", notice.shop.name)
        assertEquals(OperatingNoticeType.PLANNED_CLOSING, notice.type)
        assertEquals("15:45", notice.endTime.toString())
        assertEquals(
            "20:00",
            notice.shop.businessHoursDetails
                ?.weekly
                ?.get("sun")
                ?.close,
        )
    }
}
