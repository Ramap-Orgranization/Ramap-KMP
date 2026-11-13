package com.peto.ramap.domain.model.shop

import kotlinx.datetime.LocalDateTime

sealed interface ShopHoursFilter {
    data object All : ShopHoursFilter

    data object OpenNow : ShopHoursFilter

    data class VisitTime(
        val dateTime: LocalDateTime,
    ) : ShopHoursFilter

    fun timeAt(now: LocalDateTime): LocalDateTime? =
        when (this) {
            All -> null
            OpenNow -> now
            is VisitTime -> dateTime
        }
}
