package com.peto.ramap.domain.model.profile

import kotlin.jvm.JvmInline

@JvmInline
value class ProfileInstagram(
    val value: String,
) {
    val normalized: String?
        get() = normalize(value)

    val isValid: Boolean
        get() = normalize(value) != null

    val url: String?
        get() = url(value)

    companion object {
        const val MAX_LENGTH = 30

        private val usernamePattern = Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*")
        private val profileUrlPattern =
            Regex("(?:https?://)?(?:www\\.)?instagram\\.com/([A-Za-z0-9_.]+)/?(?:[?#][^\\s]*)?", RegexOption.IGNORE_CASE)
        private val reservedRoutes =
            setOf("about", "accounts", "api", "challenge", "developer", "developers", "direct", "emails", "explore", "legal", "nametag", "oauth", "p", "privacy", "reel", "reels", "share", "stories", "terms", "tv", "web")

        fun normalize(input: String): String? {
            val value = input.trim()
            if (value.isEmpty()) return ""
            val username = profileUrlPattern.matchEntire(value)?.groupValues?.get(1) ?: value.removePrefix("@")
            if (username.length > MAX_LENGTH || !usernamePattern.matches(username)) return null
            val canonical = username.lowercase()
            return canonical.takeUnless { it in reservedRoutes }
        }

        fun url(username: String): String? {
            if (username.isEmpty() || normalize(username) != username) return null
            return "https://www.instagram.com/$username/"
        }
    }
}
