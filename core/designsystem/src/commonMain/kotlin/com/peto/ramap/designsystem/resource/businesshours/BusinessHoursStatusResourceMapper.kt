package com.peto.ramap.designsystem.resource.businesshours

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import kotlinx.datetime.LocalDate
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.business_hours_temporary_closure_date
import ramap.shared.generated.resources.business_hours_temporary_closure_period
import ramap.shared.generated.resources.business_hours_temporary_closure_since
import ramap.shared.generated.resources.business_hours_today_closed
import ramap.shared.generated.resources.business_hours_today_closed_until
import ramap.shared.generated.resources.map_search_result_break_time_format
import ramap.shared.generated.resources.map_search_result_break_time_label
import ramap.shared.generated.resources.map_search_result_closed_format
import ramap.shared.generated.resources.map_search_result_closed_label
import ramap.shared.generated.resources.map_search_result_open
import ramap.shared.generated.resources.map_search_result_open_close_format
import ramap.shared.generated.resources.map_search_result_open_last_order_format
import ramap.shared.generated.resources.operating_notice_hours_unknown

object BusinessHoursStatusResourceMapper {
    fun noticeLabel(status: BusinessHoursStatus): UiText? =
        when (status) {
            is BusinessHoursStatus.BreakTime -> UiText(Res.string.map_search_result_break_time_label)
            is BusinessHoursStatus.TemporaryClosure -> UiText(Res.string.business_hours_today_closed)
            is BusinessHoursStatus.Closed -> UiText(Res.string.map_search_result_closed_label)
            else -> null
        }

    fun status(status: BusinessHoursStatus): UiText =
        when (status) {
            is BusinessHoursStatus.TemporaryClosure -> {
                val end = status.endDate
                if (end != null && status.startDate != end) {
                    UiText(Res.string.business_hours_today_closed_until, listOf(dateLabel(end, status.startDate.year != end.year)))
                } else {
                    UiText(Res.string.business_hours_today_closed)
                }
            }
            BusinessHoursStatus.Unknown -> UiText(Res.string.operating_notice_hours_unknown)
            BusinessHoursStatus.Open -> UiText(Res.string.map_search_result_open)
            is BusinessHoursStatus.OpenWithLastOrder ->
                UiText(Res.string.map_search_result_open_last_order_format, listOf(status.time))

            is BusinessHoursStatus.OpenUntil ->
                UiText(Res.string.map_search_result_open_close_format, listOf(status.time))

            is BusinessHoursStatus.BreakTime ->
                UiText(Res.string.map_search_result_break_time_format, listOf(status.endTime))

            is BusinessHoursStatus.Closed ->
                status.nextOpenTime?.let {
                    UiText(Res.string.map_search_result_closed_format, listOf(it))
                }
                    ?: UiText(Res.string.map_search_result_closed_label)
        }

    fun closurePeriod(
        status: BusinessHoursStatus.TemporaryClosure,
        startWeekday: String,
        endWeekday: String,
    ): UiText {
        val end = status.endDate
        val includeYear = end != null && status.startDate.year != end.year
        val start = dateLabel(status.startDate, includeYear)
        return when {
            end == null -> UiText(Res.string.business_hours_temporary_closure_since, listOf(start, startWeekday))
            end == status.startDate -> UiText(Res.string.business_hours_temporary_closure_date, listOf(start, startWeekday))
            else -> UiText(Res.string.business_hours_temporary_closure_period, listOf(start, startWeekday, dateLabel(end, includeYear), endWeekday))
        }
    }

    private fun dateLabel(
        date: LocalDate,
        includeYear: Boolean,
    ): String = if (includeYear) "${date.year}/${date.month.ordinal + 1}/${date.day}" else "${date.month.ordinal + 1}/${date.day}"
}
