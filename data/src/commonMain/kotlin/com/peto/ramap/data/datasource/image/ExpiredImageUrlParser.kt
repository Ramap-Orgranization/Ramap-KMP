package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class ExpiredImageUrlParser(
    supabaseSignedBaseUrl: String,
    private val r2AccountId: String,
    private val r2PrivateBucket: String,
) {
    private val supabaseBase = Url(supabaseSignedBaseUrl)

    fun isProtected(value: String): Boolean {
        val url = runCatching { Url(value) }.getOrNull() ?: return false
        return (url.host == supabaseBase.host && url.encodedPath.startsWith("${supabaseBase.encodedPath.trimEnd('/')}/")) ||
            (R2_ACCOUNT_PATTERN.matches(r2AccountId) && url.host == "$r2AccountId.r2.cloudflarestorage.com" && url.encodedPath.startsWith("/$r2PrivateBucket/"))
    }

    fun parse(value: String): SignedImageRequest? = parseBefore(value, null)

    fun parseExpired(
        value: String,
        now: Instant,
    ): SignedImageRequest? = parseBefore(value, now)

    private fun parseBefore(
        value: String,
        now: Instant?,
    ): SignedImageRequest? {
        val url = runCatching { Url(value) }.getOrNull() ?: return null
        if (url.protocol != URLProtocol.HTTPS || !url.user.isNullOrEmpty() || !url.password.isNullOrEmpty() || url.fragment.isNotEmpty()) return null
        return when {
            url.host == supabaseBase.host && url.port == supabaseBase.port ->
                expiredImage(url, "${supabaseBase.encodedPath.trimEnd('/')}/", supabaseExpiry(url), now)

            R2_ACCOUNT_PATTERN.matches(r2AccountId) && r2PrivateBucket.isNotBlank() && url.host == "$r2AccountId.r2.cloudflarestorage.com" && url.port == HTTPS_PORT ->
                expiredImage(url, "/$r2PrivateBucket/", r2Expiry(url), now)

            else -> null
        }
    }

    private fun expiredImage(
        url: Url,
        prefix: String,
        expiresAt: Instant?,
        now: Instant?,
    ): SignedImageRequest? {
        if (expiresAt == null || (now != null && expiresAt > now) || !url.encodedPath.startsWith(prefix)) return null
        val relativePath = url.encodedPath.removePrefix(prefix)
        val bucket = relativePath.substringBefore('/')
        val path = relativePath.substringAfter('/', "")
        return SignedImageRequest(bucket, path).takeIf { it.isSupported }
    }

    private fun supabaseExpiry(url: Url): Instant? =
        runCatching {
            val token = url.parameters.getAll(PARAM_TOKEN)?.singleOrNull() ?: return null
            val payload = token.split('.').takeIf { it.size == JWT_PART_COUNT }?.get(JWT_PAYLOAD_INDEX) ?: return null
            val json =
                Base64.UrlSafe
                    .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                    .decode(payload)
                    .decodeToString()
            val seconds =
                Json
                    .parseToJsonElement(json)
                    .jsonObject[JWT_EXPIRY]
                    ?.jsonPrimitive
                    ?.longOrNull ?: return null
            Instant.fromEpochSeconds(seconds)
        }.getOrNull()

    private fun r2Expiry(url: Url): Instant? =
        runCatching {
            if (url.parameters.getAll(PARAM_ALGORITHM)?.singleOrNull() != AWS_SIGNATURE_ALGORITHM) return null
            if (url.parameters
                    .getAll(PARAM_SIGNATURE)
                    ?.singleOrNull()
                    .isNullOrBlank()
            ) {
                return null
            }
            val date = url.parameters.getAll(PARAM_DATE)?.singleOrNull() ?: return null
            if (!AWS_DATE_PATTERN.matches(date)) return null
            val lifetime =
                url.parameters
                    .getAll(PARAM_EXPIRES)
                    ?.singleOrNull()
                    ?.toLongOrNull() ?: return null
            if (lifetime !in 1..MAX_SIGNED_LIFETIME_SECONDS) return null
            val issuedAt = Instant.parse("${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}T${date.substring(9, 11)}:${date.substring(11, 13)}:${date.substring(13, 15)}Z")
            issuedAt + lifetime.seconds
        }.getOrNull()

    private companion object {
        const val HTTPS_PORT = 443
        const val JWT_PART_COUNT = 3
        const val JWT_PAYLOAD_INDEX = 1
        const val JWT_EXPIRY = "exp"
        const val PARAM_TOKEN = "token"
        const val PARAM_ALGORITHM = "X-Amz-Algorithm"
        const val PARAM_SIGNATURE = "X-Amz-Signature"
        const val PARAM_DATE = "X-Amz-Date"
        const val PARAM_EXPIRES = "X-Amz-Expires"
        const val AWS_SIGNATURE_ALGORITHM = "AWS4-HMAC-SHA256"
        const val MAX_SIGNED_LIFETIME_SECONDS = 360L
        val R2_ACCOUNT_PATTERN = Regex("[a-f0-9]{32}")
        val AWS_DATE_PATTERN = Regex("[0-9]{8}T[0-9]{6}Z")
    }
}
