package com.peto.ramap.domain.model.shop

import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessHoursTest {
    @Test
    fun `영업시간 데이터의 의미 필드를 보존한다`() {
        val hours =
            BusinessHours(
                weekly = mapOf("sat" to BusinessHoursDay(false, "11:00", "16:00", false, null)),
                breakTimes = mapOf("sat" to listOf(BusinessHoursBreakTime("13:00", "14:00"))),
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
}
