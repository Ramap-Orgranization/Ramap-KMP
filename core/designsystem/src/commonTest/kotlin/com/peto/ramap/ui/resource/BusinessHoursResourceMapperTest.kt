package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.shop.BusinessHours
import com.peto.ramap.domain.model.shop.BusinessHoursBreakTime
import com.peto.ramap.domain.model.shop.BusinessHoursDay
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursResourceMapper
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_detail_business_hours_break_time_format
import ramap.shared.generated.resources.shop_detail_business_hours_closed
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_fri
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_mon
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_sat
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_sun
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_wed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BusinessHoursResourceMapperTest {
    @Test
    fun `같은 월요일부터 금요일은 범위로 묶고 토요일과 일요일 휴무는 분리한다`() {
        val days =
            mapOf(
                "mon" to openDay(),
                "tue" to openDay(),
                "wed" to openDay(),
                "thu" to openDay(),
                "fri" to openDay(),
                "sat" to openDay(close = "17:00"),
                "sun" to closedDay(),
            )
        val lines = BusinessHoursResourceMapper.all(businessHours(days = days))

        assertEquals(3, lines.size)
        assertEquals(Res.string.shop_detail_business_hours_weekday_mon, lines[0].dayLabel)
        assertEquals(Res.string.shop_detail_business_hours_weekday_fri, lines[0].endDayLabel)
        assertEquals(Res.string.shop_detail_business_hours_weekday_sat, lines[1].dayLabel)
        assertNull(lines[1].endDayLabel)
        assertEquals(Res.string.shop_detail_business_hours_weekday_sun, lines[2].dayLabel)
        assertEquals(Res.string.shop_detail_business_hours_closed, lines[2].values.single().resource)
    }

    @Test
    fun `요일별 브레이크타임이 다르면 같은 영업시간이어도 분리한다`() {
        val days = mapOf("mon" to openDay(), "tue" to openDay())
        val breakTimes =
            mapOf(
                "mon" to listOf(BusinessHoursBreakTime("14:00", "15:00")),
                "tue" to listOf(BusinessHoursBreakTime("15:00", "16:00")),
            )
        val lines = BusinessHoursResourceMapper.all(businessHours(days = days, breakTimes = breakTimes))

        assertEquals(2, lines.size)
        assertNull(lines[0].endDayLabel)
        assertEquals(Res.string.shop_detail_business_hours_break_time_format, lines[0].values[1].resource)
    }

    @Test
    fun `weekly에 없는 요일은 인접한 동일 패턴을 합치는 경계가 된다`() {
        val days = mapOf("mon" to openDay(), "wed" to openDay())
        val lines = BusinessHoursResourceMapper.all(businessHours(days = days))

        assertEquals(2, lines.size)
        assertEquals(Res.string.shop_detail_business_hours_weekday_mon, lines[0].dayLabel)
        assertEquals(Res.string.shop_detail_business_hours_weekday_wed, lines[1].dayLabel)
        assertNull(lines[0].endDayLabel)
    }

    private fun businessHours(
        days: Map<String, BusinessHoursDay>,
        breakTimes: Map<String, List<BusinessHoursBreakTime>> = emptyMap(),
    ) = BusinessHours(
        weekly = days,
        breakTimes = breakTimes,
        lastOrders = emptyMap(),
        notice = null,
    )

    private fun openDay(close: String = "18:00") = BusinessHoursDay(false, "11:00", close, false, null)

    private fun closedDay() = BusinessHoursDay(true, null, null, false, null)
}
