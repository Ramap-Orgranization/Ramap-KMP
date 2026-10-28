package com.peto.ramap.designsystem.review

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun reviewDateLabel(
    createdAt: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String =
    runCatching {
        Instant
            .parse(createdAt)
            .toLocalDateTime(timeZone)
            .date
            .toString()
    }.getOrElse { createdAt.take(10) }.replace('-', '.')
