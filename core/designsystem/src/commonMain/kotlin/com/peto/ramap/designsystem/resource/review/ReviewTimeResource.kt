package com.peto.ramap.designsystem.resource.review

import com.peto.ramap.datetime.toDotFormatted
import com.peto.ramap.datetime.toLocalDateOrNull
import com.peto.ramap.designsystem.resource.UiText
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_time_date
import ramap.shared.generated.resources.review_time_days_ago
import ramap.shared.generated.resources.review_time_hours_ago
import ramap.shared.generated.resources.review_time_just_now
import ramap.shared.generated.resources.review_time_minutes_ago
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val reviewDateTimeFormat =
    LocalDateTime.Format {
        year()
        char('.')
        monthNumber()
        char('.')
        day()
        char(' ')
        hour()
        char(':')
        minute()
    }

internal fun reviewCreationTime(
    createdAt: String,
    timeZone: TimeZone,
): Instant? =
    Instant.parseOrNull(createdAt)
        ?: createdAt.toLocalDateOrNull()?.atStartOfDayIn(timeZone)

internal fun reviewDateTimeResource(
    createdAt: String,
    timeZone: TimeZone,
): UiText? {
    val writtenAt = Instant.parseOrNull(createdAt)
    val label =
        if (writtenAt != null) {
            reviewDateTimeFormat.format(writtenAt.toLocalDateTime(timeZone))
        } else {
            createdAt.toLocalDateOrNull()?.toDotFormatted() ?: return null
        }
    return UiText(Res.string.review_time_date, listOf(label))
}

internal fun reviewTimeResource(
    writtenAt: Instant,
    now: Instant,
    timeZone: TimeZone,
): UiText {
    val elapsed = now - writtenAt
    return when {
        elapsed >= 7.days -> {
            val date =
                writtenAt
                    .toLocalDateTime(timeZone)
                    .date
                    .toDotFormatted()
            UiText(Res.string.review_time_date, listOf(date))
        }

        elapsed >= 1.days -> UiText(Res.string.review_time_days_ago, listOf(elapsed.inWholeDays))
        elapsed >= 1.hours -> UiText(Res.string.review_time_hours_ago, listOf(elapsed.inWholeHours))
        elapsed >= 1.minutes -> UiText(Res.string.review_time_minutes_ago, listOf(elapsed.inWholeMinutes))
        else -> UiText(Res.string.review_time_just_now)
    }
}
