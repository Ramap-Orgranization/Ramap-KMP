package com.peto.ramap.domain.model.shop

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
value class ShopOpeningMonth private constructor(
    val firstDay: LocalDate,
) {
    val monthNumber: Int get() = firstDay.month.ordinal + 1
    val endExclusive: LocalDate get() = firstDay.plus(1, DateTimeUnit.MONTH)

    companion object {
        private val SEOUL = TimeZone.of("Asia/Seoul")

        fun containing(instant: Instant): ShopOpeningMonth {
            val date = instant.toLocalDateTime(SEOUL).date
            return ShopOpeningMonth(LocalDate(date.year, date.month, 1))
        }
    }
}
