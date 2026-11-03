package com.peto.ramap.network.client.media

import kotlin.time.Instant

data class MediaImageUrl(
    val path: String,
    val url: String,
    val expiresAt: Instant,
)
