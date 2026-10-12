package com.peto.ramap.data.repository

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal data class AvatarCache(
    val userId: String,
    val path: String,
    val url: String,
    val expiresAt: Instant,
) {
    fun matches(
        userId: String,
        path: String,
        currentTime: Instant,
    ): Boolean = this.userId == userId && this.path == path && currentTime < expiresAt - REFRESH_MARGIN

    private companion object {
        val REFRESH_MARGIN = 1.minutes
    }
}
