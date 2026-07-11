package com.peto.ramap.domain.model

object PlaceReportTextParser {
    fun extractSupportedUrl(content: String): String? =
        Regex(URL_PATTERN, RegexOption.IGNORE_CASE)
            .findAll(content)
            .map { it.value.trimEnd('.', ',', ')', ']', '}', '>', '\"', '\'') }
            .firstOrNull(::isSupportedUrl)

    fun extractSharedPlaceName(content: String): String? =
        content
            .lineSequence()
            .map(String::trim)
            .firstOrNull { line -> SHARE_PREFIXES.any(line::startsWith) }
            ?.substringAfter(']')
            ?.trim()
            ?.takeIf(String::isNotEmpty)

    fun matchesSharedPlace(
        content: String,
        shop: RamenShop,
    ): Boolean {
        val normalizedContent = content.filterNot(Char::isWhitespace).lowercase()
        val sameUrl =
            listOfNotNull(shop.kakaoPlaceUrl, shop.naverPlaceUrl)
                .any { it.equals(extractSupportedUrl(content), ignoreCase = true) }
        val sameName = extractSharedPlaceName(content)?.equals(shop.name, ignoreCase = true) == true
        val sameAddress = shop.address.filterNot(Char::isWhitespace).lowercase() in normalizedContent

        return sameUrl || sameName || sameAddress
    }

    private fun isSupportedUrl(url: String): Boolean {
        val value = url.trim().lowercase()
        val host =
            when {
                value.startsWith(HTTP_PREFIX) -> value.removePrefix(HTTP_PREFIX).substringBefore(PATH_SEPARATOR)
                value.startsWith(HTTPS_PREFIX) -> value.removePrefix(HTTPS_PREFIX).substringBefore(PATH_SEPARATOR)
                else -> return false
            }.substringBefore(PORT_SEPARATOR)

        return host in EXACT_HOSTS || HOST_SUFFIXES.any(host::endsWith)
    }

    private const val HTTP_PREFIX = "http://"
    private const val HTTPS_PREFIX = "https://"
    private const val PATH_SEPARATOR = "/"
    private const val PORT_SEPARATOR = ":"
    private const val URL_PATTERN = "https?://\\S+"
    private val SHARE_PREFIXES = listOf("[카카오맵]", "[네이버지도]")
    private val EXACT_HOSTS = setOf("kko.to", "naver.me")
    private val HOST_SUFFIXES = listOf(".kakao.com", ".naver.com")
}
