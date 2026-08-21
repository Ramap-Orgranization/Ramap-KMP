package com.peto.ramap.data.extension

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

internal fun String.toLocalDate(): LocalDate =
    runCatching { LocalDate.parse(this) }
        .getOrElse { error("Invalid date format: $this") }

internal fun String.toLocalTime(): LocalTime =
    runCatching { LocalTime.parse(this) }
        .getOrElse { error("Invalid time format: $this") }
