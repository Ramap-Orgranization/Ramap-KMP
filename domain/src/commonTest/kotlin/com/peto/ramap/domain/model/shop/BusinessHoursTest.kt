package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.businesshour.BreakTime
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessHoursTest {
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
                        lastOrders = mapOf("mon" to listOf("21:00")),
                        notice = null,
                    ),
            )

        assertEquals(
            BusinessHoursStatus.OpenWithLastOrder("21:00"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 10, 13, 0)),
        )
        assertEquals(
            BusinessHoursStatus.BreakTime("15:00"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 10, 14, 30)),
        )
        assertEquals(
            BusinessHoursStatus.Closed("12:00"),
            shop.businessHoursStatus(LocalDateTime(2026, 8, 10, 23, 0)),
        )
    }
}
