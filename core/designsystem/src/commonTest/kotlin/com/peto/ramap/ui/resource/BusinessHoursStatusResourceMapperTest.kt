package com.peto.ramap.ui.resource

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursStatusResourceMapper
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_search_result_closed_format
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
            UiText(Res.string.map_search_result_closed_format, listOf("12:00")),
            BusinessHoursStatusResourceMapper.status(BusinessHoursStatus.Closed("12:00")),
        )
    }
}
