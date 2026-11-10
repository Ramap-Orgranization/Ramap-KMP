package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.datetime.toDotFormatted
import com.peto.ramap.datetime.toLocalDateOrNull
import com.peto.ramap.domain.model.notice.OperatingNoticeType

internal fun toOperatingNoticeType(value: String): OperatingNoticeType? = runCatching { OperatingNoticeType.from(value) }.getOrNull()

internal fun formatDateRange(
    startDate: String?,
    endDate: String?,
): String? {
    if (startDate == null) return null
    val startDot = startDate.toLocalDateOrNull()?.toDotFormatted() ?: startDate
    val endDot = endDate?.toLocalDateOrNull()?.toDotFormatted() ?: endDate
    return when {
        endDot == null || startDot == endDot -> startDot
        else -> "$startDot ~ $endDot"
    }
}
