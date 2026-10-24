package com.peto.ramap.domain.model.businesshour

import kotlinx.datetime.LocalDate

sealed interface BusinessHoursStatus {
    val isNotOpening: Boolean
        get() = this is BreakTime || this is Closed || this is TemporaryClosure || this is Unknown

    data object Unknown : BusinessHoursStatus

    data object Open : BusinessHoursStatus

    data class OpenWithLastOrder(
        val time: String,
    ) : BusinessHoursStatus

    data class OpenUntil(
        val time: String,
    ) : BusinessHoursStatus

    data class BreakTime(
        val endTime: String,
    ) : BusinessHoursStatus

    data class TemporaryClosure(
        val startDate: LocalDate,
        val endDate: LocalDate?,
    ) : BusinessHoursStatus

    data class Closed(
        val nextOpenTime: String? = null,
    ) : BusinessHoursStatus
}
