package com.peto.ramap.domain.model.businesshour

sealed interface BusinessHoursStatus {
    val isNotOpening: Boolean
        get() = this is BreakTime || this is Closed

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

    data class Closed(
        val nextOpenTime: String,
    ) : BusinessHoursStatus
}
