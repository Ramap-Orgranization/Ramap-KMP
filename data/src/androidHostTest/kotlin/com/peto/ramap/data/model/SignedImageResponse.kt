package com.peto.ramap.data.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class SignedImageResponse(
    val bucket: String,
    val path: String,
    val url: String? = null,
    val expiresAt: Long? = null,
) {
    fun toSignedImageUrl(): SignedImageUrl? {
        val url = url ?: return null
        val expiresAt = expiresAt ?: return null
        return SignedImageUrl(
            url = url,
            expiresAt = Instant.fromEpochMilliseconds(expiresAt),
        )
    }
}
