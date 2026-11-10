package com.peto.ramap.data.auth

import com.peto.ramap.domain.model.auth.SignupTerms
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
internal class SignupTermsChallenge(
    @SerialName("challenge_id") val id: String,
    private val version: String,
    private val url: String,
) {
    val terms: SignupTerms
        get() = SignupTerms(version, url)

    @OptIn(ExperimentalUuidApi::class)
    private fun isValid(): Boolean {
        val uri = runCatching { Url(url) }.getOrNull() ?: return false
        return VERSION_PATTERN.matches(version) &&
            runCatching { Uuid.parse(id) }.isSuccess &&
            url.startsWith("https://") &&
            uri.protocol == URLProtocol.HTTPS &&
            uri.host.isNotBlank() &&
            uri.user == null &&
            uri.password == null
    }

    companion object {
        private const val REQUIRED_PREFIX = "RAMAP_SIGNUP_TERMS_REQUIRED "
        private val VERSION_PATTERN = Regex("[a-zA-Z0-9._-]{1,64}")

        fun isChallengeMessage(message: String?): Boolean = message?.startsWith(REQUIRED_PREFIX) == true

        fun decode(message: String): SignupTermsChallenge? =
            runCatching {
                Json.decodeFromString<SignupTermsChallenge>(message.removePrefix(REQUIRED_PREFIX)).takeIf { it.isValid() }
            }.getOrNull()
    }
}
