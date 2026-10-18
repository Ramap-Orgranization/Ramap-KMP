package com.peto.ramap.data.datasource.community

import kotlin.time.Instant

internal data class ProfileAvatarUrl(
    val viewerId: String?,
    val path: String,
    val url: String,
    val refreshAt: Instant,
) {
    fun matches(
        viewerId: String?,
        path: String,
        now: Instant,
    ): Boolean = this.viewerId == viewerId && this.path == path && now < refreshAt
}
