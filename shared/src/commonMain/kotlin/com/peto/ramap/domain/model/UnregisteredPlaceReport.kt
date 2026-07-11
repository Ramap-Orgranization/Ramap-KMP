package com.peto.ramap.domain.model

data class UnregisteredPlaceReport(
    val placeUrl: String? = null,
    val location: Location? = null,
)

fun String.isSupportedPlaceReportUrl(): Boolean {
    val value = trim().lowercase()
    val host =
        when {
            value.startsWith(HTTP_PREFIX) -> value.removePrefix(HTTP_PREFIX).substringBefore(PATH_SEPARATOR)
            value.startsWith(HTTPS_PREFIX) -> value.removePrefix(HTTPS_PREFIX).substringBefore(PATH_SEPARATOR)
            else -> return false
        }.substringBefore(PORT_SEPARATOR)

    return host.isSupportedPlaceReportHost()
}

fun String.extractSupportedPlaceReportUrl(): String? =
    Regex(URL_PATTERN, RegexOption.IGNORE_CASE)
        .findAll(this)
        .map { it.value.trimEnd('.', ',', ')', ']', '}', '>', '\"', '\'') }
        .firstOrNull { it.isSupportedPlaceReportUrl() }

fun String.extractSharedPlaceName(): String? =
    lineSequence()
        .map(String::trim)
        .firstOrNull { line -> SHARE_PREFIXES.any(line::startsWith) }
        ?.substringAfter(']')
        ?.trim()
        ?.takeIf(String::isNotEmpty)

fun String.matchesSharedPlace(shop: RamenShop): Boolean {
    val normalizedContent = filterNot(Char::isWhitespace).lowercase()
    val sameUrl =
        listOfNotNull(shop.kakaoPlaceUrl, shop.naverPlaceUrl)
            .any { it.equals(extractSupportedPlaceReportUrl(), ignoreCase = true) }
    val sameName = extractSharedPlaceName()?.equals(shop.name, ignoreCase = true) == true
    val sameAddress = shop.address.filterNot(Char::isWhitespace).lowercase() in normalizedContent

    return sameUrl || sameName || sameAddress
}

private fun String.isSupportedPlaceReportHost(): Boolean = this in EXACT_HOSTS || HOST_SUFFIXES.any(::endsWith)

private const val HTTP_PREFIX = "http://"
private const val HTTPS_PREFIX = "https://"
private const val PATH_SEPARATOR = "/"
private const val PORT_SEPARATOR = ":"
private const val URL_PATTERN = "https?://\\S+"
private val SHARE_PREFIXES = listOf("[카카오맵]", "[네이버지도]")
private val EXACT_HOSTS = setOf("kko.to", "naver.me")
private val HOST_SUFFIXES = listOf(".kakao.com", ".naver.com")
