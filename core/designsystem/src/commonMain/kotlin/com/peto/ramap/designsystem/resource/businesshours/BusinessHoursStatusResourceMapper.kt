package com.peto.ramap.designsystem.resource.businesshours

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_search_result_break_time_format
import ramap.shared.generated.resources.map_search_result_break_time_label
import ramap.shared.generated.resources.map_search_result_closed_format
import ramap.shared.generated.resources.map_search_result_closed_label
import ramap.shared.generated.resources.map_search_result_open
import ramap.shared.generated.resources.map_search_result_open_close_format
import ramap.shared.generated.resources.map_search_result_open_last_order_format

object BusinessHoursStatusResourceMapper {
    fun noticeLabel(status: BusinessHoursStatus): UiText? =
        when (status) {
            is BusinessHoursStatus.BreakTime -> UiText(Res.string.map_search_result_break_time_label)
            is BusinessHoursStatus.Closed -> UiText(Res.string.map_search_result_closed_label)
            else -> null
        }

    fun status(status: BusinessHoursStatus): UiText =
        when (status) {
            BusinessHoursStatus.Open -> UiText(Res.string.map_search_result_open)
            is BusinessHoursStatus.OpenWithLastOrder ->
                UiText(Res.string.map_search_result_open_last_order_format, listOf(status.time))

            is BusinessHoursStatus.OpenUntil ->
                UiText(Res.string.map_search_result_open_close_format, listOf(status.time))

            is BusinessHoursStatus.BreakTime ->
                UiText(Res.string.map_search_result_break_time_format, listOf(status.endTime))

            is BusinessHoursStatus.Closed ->
                UiText(Res.string.map_search_result_closed_format, listOf(status.nextOpenTime))
        }
}
