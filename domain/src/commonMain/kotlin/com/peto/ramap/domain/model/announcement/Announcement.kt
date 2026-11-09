package com.peto.ramap.domain.model.announcement

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class Announcement(
    val id: String,
    val title: String,
    val bodyMarkdown: String,
    val publishedAt: Instant,
) {
    fun isUnread(readIds: Set<String>): Boolean = id !in readIds

    fun formattedPublishedDate(
        delimiter: String = ".",
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String =
        publishedAt
            .toLocalDateTime(timeZone)
            .date
            .toString()
            .replace("-", delimiter)
}
