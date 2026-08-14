package com.peto.ramap.designsystem.resource.businesshours

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_search_result_closed_format
import ramap.shared.generated.resources.map_search_result_open
import ramap.shared.generated.resources.map_search_result_open_close_format
import ramap.shared.generated.resources.map_search_result_open_last_order_format

object BusinessHoursStatusResourceMapper {
    fun status(status: BusinessHoursStatus): UiText =
        when (status) {
            BusinessHoursStatus.Open -> UiText(Res.string.map_search_result_open)
            is BusinessHoursStatus.OpenWithLastOrder ->
                UiText(Res.string.map_search_result_open_last_order_format, listOf(status.time))

            is BusinessHoursStatus.OpenUntil ->
                UiText(Res.string.map_search_result_open_close_format, listOf(status.time))

            is BusinessHoursStatus.Closed ->
                UiText(Res.string.map_search_result_closed_format, listOf(status.nextOpenTime))
        }
}
