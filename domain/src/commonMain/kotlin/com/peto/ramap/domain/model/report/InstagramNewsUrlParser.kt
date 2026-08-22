package com.peto.ramap.domain.model.report

object InstagramNewsUrlParser {
    fun extractCanonicalUrl(content: String): String? {
        val match = URL_PATTERN.find(content) ?: return null
        val url = match.value.trimEnd('.', ',', ')', ']', '}', '>', '"', '\'')
        val path = url.substringAfter("instagram.com/")
        val type = path.substringBefore('/')
        val shortcode = path.substringAfter('/', "").takeIf(String::isNotBlank) ?: return null
        return "https://www.instagram.com/${type.lowercase()}/$shortcode"
    }

    private val URL_PATTERN =
        Regex(
            "https?://(?:www\\.)?instagram\\.com/(?:p|reel)/[^/?#\\s]+",
            RegexOption.IGNORE_CASE,
        )
}
