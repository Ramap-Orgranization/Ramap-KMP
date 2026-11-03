package com.peto.ramap.network.client.media

import kotlin.jvm.JvmInline

@JvmInline
value class MediaPath(
    val value: String,
) {
    val isValid: Boolean
        get() =
            value.isNotBlank() &&
                value == value.trim() &&
                !value.startsWith(PATH_SEPARATOR_CHAR) &&
                !value.contains(PATH_TRAVERSAL) &&
                !value.any { it in DISALLOWED_PATH_CHARS } &&
                !value.contains(URL_SCHEME_SEPARATOR)

    companion object {
        private const val PATH_SEPARATOR_CHAR = '/'
        private const val PATH_TRAVERSAL = ".."
        private const val DISALLOWED_PATH_CHARS = "\\%?#"
        private const val URL_SCHEME_SEPARATOR = "://"

        fun from(path: String?): MediaPath? {
            if (path.isNullOrBlank()) return null
            val mediaPath = MediaPath(path.trim())
            return if (mediaPath.isValid) mediaPath else null
        }
    }
}
