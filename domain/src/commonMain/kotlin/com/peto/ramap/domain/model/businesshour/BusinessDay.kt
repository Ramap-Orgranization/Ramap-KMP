package com.peto.ramap.domain.model.businesshour

import kotlinx.datetime.DayOfWeek
import kotlin.jvm.JvmInline

@JvmInline
value class BusinessDay(
    val dayOfWeek: DayOfWeek,
) {
    val key: String
        get() =
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> "mon"
                DayOfWeek.TUESDAY -> "tue"
                DayOfWeek.WEDNESDAY -> "wed"
                DayOfWeek.THURSDAY -> "thu"
                DayOfWeek.FRIDAY -> "fri"
                DayOfWeek.SATURDAY -> "sat"
                DayOfWeek.SUNDAY -> "sun"
            }

    fun previous(): BusinessDay = BusinessDay(DayOfWeek.entries[(dayOfWeek.ordinal + 6) % 7])

    companion object {
        fun from(dayOfWeek: DayOfWeek): BusinessDay = BusinessDay(dayOfWeek)
    }
}
