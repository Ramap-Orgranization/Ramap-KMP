package com.peto.ramap.data.model

import com.peto.ramap.domain.model.announcement.Announcement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class AnnouncementResponse(
    val id: String,
    val title: String,
    @SerialName("body_markdown") val bodyMarkdown: String,
    @SerialName("published_at") val publishedAt: Instant,
) {
    fun toDomain(): Announcement = Announcement(id, title, bodyMarkdown, publishedAt)
}
