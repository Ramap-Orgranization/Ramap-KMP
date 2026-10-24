package com.peto.ramap.ui.resource

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursStatusResourceMapper
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import kotlinx.datetime.LocalDate
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.business_hours_temporary_closure_date
import ramap.shared.generated.resources.business_hours_temporary_closure_period
import ramap.shared.generated.resources.business_hours_temporary_closure_since
import ramap.shared.generated.resources.business_hours_today_closed
import ramap.shared.generated.resources.business_hours_today_closed_until
import ramap.shared.generated.resources.map_search_result_break_time_format
import ramap.shared.generated.resources.map_search_result_closed_format
import ramap.shared.generated.resources.map_search_result_closed_label
import ramap.shared.generated.resources.map_search_result_open
import ramap.shared.generated.resources.map_search_result_open_close_format
import ramap.shared.generated.resources.map_search_result_open_last_order_format
import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessHoursStatusResourceMapperTest {
    @Test
    fun `영업 상태를 리소스와 인자로 매핑한다`() {
        assertEquals(
            UiText(Res.string.map_search_result_open),
            BusinessHoursStatusResourceMapper.status(BusinessHoursStatus.Open),
        )
        assertEquals(
            UiText(Res.string.map_search_result_open_last_order_format, listOf("21:00")),
            BusinessHoursStatusResourceMapper.status(BusinessHoursStatus.OpenWithLastOrder("21:00")),
        )
        assertEquals(
            UiText(Res.string.map_search_result_open_close_format, listOf("22:00")),
            BusinessHoursStatusResourceMapper.status(BusinessHoursStatus.OpenUntil("22:00")),
        )
        assertEquals(
            UiText(Res.string.map_search_result_break_time_format, listOf("15:00")),
            BusinessHoursStatusResourceMapper.status(BusinessHoursStatus.BreakTime("15:00")),
        )
        assertEquals(
            UiText(Res.string.map_search_result_closed_format, listOf("12:00")),
            BusinessHoursStatusResourceMapper.status(BusinessHoursStatus.Closed("12:00")),
        )
        assertEquals(
            UiText(Res.string.map_search_result_closed_label),
            BusinessHoursStatusResourceMapper.status(BusinessHoursStatus.Closed()),
        )
    }

    @Test
    fun `하루 휴무는 오늘 휴무와 해당 날짜를 표시한다`() {
        val date = LocalDate(2026, 10, 24)
        val status = BusinessHoursStatus.TemporaryClosure(date, date)
        assertEquals(UiText(Res.string.business_hours_today_closed), BusinessHoursStatusResourceMapper.status(status))
        assertEquals(UiText(Res.string.business_hours_today_closed), BusinessHoursStatusResourceMapper.noticeLabel(status))
        assertEquals(UiText(Res.string.business_hours_temporary_closure_date, listOf("10/24", "토")), BusinessHoursStatusResourceMapper.closurePeriod(status, "토", "토"))
    }

    @Test
    fun `연속 휴무는 종료일과 전체 기간을 표시한다`() {
        val status = BusinessHoursStatus.TemporaryClosure(LocalDate(2026, 10, 24), LocalDate(2026, 10, 30))
        assertEquals(UiText(Res.string.business_hours_today_closed_until, listOf("10/30")), BusinessHoursStatusResourceMapper.status(status))
        assertEquals(UiText(Res.string.business_hours_temporary_closure_period, listOf("10/24", "토", "10/30", "금")), BusinessHoursStatusResourceMapper.closurePeriod(status, "토", "금"))
    }

    @Test
    fun `종료일 없는 휴무는 시작일과 요일만 안내한다`() {
        val status = BusinessHoursStatus.TemporaryClosure(LocalDate(2026, 10, 24), null)
        assertEquals(
            UiText(Res.string.business_hours_temporary_closure_since, listOf("10/24", "토")),
            BusinessHoursStatusResourceMapper.closurePeriod(status, "토", "토"),
        )
    }

    @Test
    fun `해를 넘는 휴무는 연도를 포함해 기간을 구분한다`() {
        val status = BusinessHoursStatus.TemporaryClosure(LocalDate(2026, 12, 30), LocalDate(2027, 1, 2))
        assertEquals(UiText(Res.string.business_hours_today_closed_until, listOf("2027/1/2")), BusinessHoursStatusResourceMapper.status(status))
        assertEquals(UiText(Res.string.business_hours_temporary_closure_period, listOf("2026/12/30", "수", "2027/1/2", "토")), BusinessHoursStatusResourceMapper.closurePeriod(status, "수", "토"))
    }
}
