package com.peto.ramap.designsystem.resource.review

import com.peto.ramap.designsystem.resource.UiText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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

internal fun reviewCreationTime(
    createdAt: String,
    timeZone: TimeZone,
): Instant? =
    Instant.parseOrNull(createdAt)
        ?: runCatching { LocalDate.parse(createdAt).atStartOfDayIn(timeZone) }.getOrNull()

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
                    .toString()
                    .replace('-', '.')
            UiText(Res.string.review_time_date, listOf(date))
        }

        elapsed >= 1.days -> UiText(Res.string.review_time_days_ago, listOf(elapsed.inWholeDays))
        elapsed >= 1.hours -> UiText(Res.string.review_time_hours_ago, listOf(elapsed.inWholeHours))
        elapsed >= 1.minutes -> UiText(Res.string.review_time_minutes_ago, listOf(elapsed.inWholeMinutes))
        else -> UiText(Res.string.review_time_just_now)
    }
}
