package com.peto.ramap.network.client.media

import com.peto.ramap.network.config.RamapSecrets
import io.ktor.http.Url

class PublicMediaUrl(
    private val bucket: MediaBucket,
    private val path: MediaPath,
    private val publicBaseUrl: String = RamapSecrets.r2PublicBaseUrl,
) {
    fun resolve(): String? {
        if (!bucket.isPublic ||
            !bucket.isValid ||
            !path.isValid
        ) {
            return null
        }
        val mediaBase = publicBaseUrl.trimEnd(PATH_SEPARATOR_CHAR)
        if (mediaBase.isBlank()) return null
        val base = runCatching { Url(mediaBase) }.getOrNull() ?: return null
        if (base.protocol.name != HTTPS_SCHEME ||
            base.port != HTTPS_DEFAULT_PORT ||
            !base.user.isNullOrEmpty() ||
            !base.password.isNullOrEmpty() ||
            base.parameters.isEmpty().not() ||
            base.fragment.isNotEmpty()
        ) {
            return null
        }
        if (base.encodedPath !in VALID_ENCODED_PATHS ||
            base.host.endsWith(R2_DEV_HOST_SUFFIX) ||
            base.host.endsWith(R2_STORAGE_HOST_SUFFIX)
        ) {
            return null
        }
        return "$mediaBase/${bucket.value}/${path.value}"
    }

    companion object {
        const val SUPABASE_PUBLIC_STORAGE_PATH = "/storage/v1/object/public"

        private const val HTTPS_SCHEME = "https"
        private const val HTTPS_DEFAULT_PORT = 443
        private const val R2_DEV_HOST_SUFFIX = ".r2.dev"
        private const val R2_STORAGE_HOST_SUFFIX = ".r2.cloudflarestorage.com"
        private const val PATH_SEPARATOR_CHAR = '/'

        private val VALID_ENCODED_PATHS = setOf("", "/")

        fun resolveOwnedUrl(url: String?): String? {
            if (url == null) return null
            val supabaseBase = RamapSecrets.supabaseUrl.trimEnd(PATH_SEPARATOR_CHAR)
            if (supabaseBase.isBlank()) return url
            val prefix = "$supabaseBase$SUPABASE_PUBLIC_STORAGE_PATH/"
            if (!url.startsWith(prefix)) return url
            val location = url.removePrefix(prefix)
            return PublicMediaUrl(
                bucket = MediaBucket(location.substringBefore(PATH_SEPARATOR_CHAR)),
                path = MediaPath(location.substringAfter(PATH_SEPARATOR_CHAR, "")),
                publicBaseUrl = RamapSecrets.r2PublicBaseUrl,
            ).resolve()
        }
    }
}
