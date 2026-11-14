package com.peto.ramap.domain.model.shop

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
value class ShopRegistrationMonth private constructor(
    private val firstDay: LocalDate,
) {
    val monthNumber: Int get() = firstDay.month.ordinal + 1
    val start: Instant get() = firstDay.atStartOfDayIn(SEOUL)
    val endExclusive: Instant get() = firstDay.plus(1, DateTimeUnit.MONTH).atStartOfDayIn(SEOUL)

    companion object {
        private val SEOUL = TimeZone.of("Asia/Seoul")

        fun containing(instant: Instant): ShopRegistrationMonth {
            val date = instant.toLocalDateTime(SEOUL).date
            return ShopRegistrationMonth(LocalDate(date.year, date.month, 1))
        }
    }
}
