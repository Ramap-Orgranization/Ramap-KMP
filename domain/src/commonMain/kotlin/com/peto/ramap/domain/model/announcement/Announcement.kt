package com.peto.ramap.domain.model.announcement

import kotlin.time.Instant

data class Announcement(
    val id: String,
    val title: String,
    val bodyMarkdown: String,
    val publishedAt: Instant,
) {
    fun isUnread(readIds: Set<String>): Boolean = id !in readIds
}
