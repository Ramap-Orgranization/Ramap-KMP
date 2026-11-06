package com.peto.ramap.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SignedImageRequest(
    val bucket: String,
    val path: String,
) {
    val isSupported: Boolean
        get() =
            when (bucket) {
                "profile-avatars", "shop-review-images" -> IMAGE_PATH.matches(path)
                "news-report-evidence" -> EVIDENCE_PATH.matches(path)
                else -> false
            }

    private companion object {
        const val UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        val IMAGE_PATH = Regex("$UUID/$UUID\\.(jpg|png)", RegexOption.IGNORE_CASE)
        val EVIDENCE_PATH = Regex("($UUID/)?$UUID\\.(jpg|png)", RegexOption.IGNORE_CASE)
    }
}
