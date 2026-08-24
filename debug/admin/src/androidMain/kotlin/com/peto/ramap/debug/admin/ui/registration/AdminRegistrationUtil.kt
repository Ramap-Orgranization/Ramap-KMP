package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.domain.model.notice.OperatingNoticeType

internal fun toOperatingNoticeType(value: String): OperatingNoticeType? = runCatching { OperatingNoticeType.from(value) }.getOrNull()

internal fun formatDateRange(
    startDate: String?,
    endDate: String?,
): String? =
    when {
        startDate == null -> null
        endDate == null || startDate == endDate -> startDate
        else -> "$startDate ~ $endDate"
    }
