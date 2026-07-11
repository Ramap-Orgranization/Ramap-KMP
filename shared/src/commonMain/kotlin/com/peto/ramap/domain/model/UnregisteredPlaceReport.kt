package com.peto.ramap.domain.model

data class UnregisteredPlaceReport(
    val placeUrl: String? = null,
    val location: Location? = null,
)

fun String.isSupportedPlaceReportUrl(): Boolean {
    val value = trim().lowercase()
    val host =
        when {
            value.startsWith("http://") -> value.removePrefix("http://").substringBefore("/")
            value.startsWith("https://") -> value.removePrefix("https://").substringBefore("/")
            else -> return false
        }.substringBefore(":")

    return host.isSupportedPlaceReportHost()
}

fun String.extractSupportedPlaceReportUrl(): String? =
    Regex("https?://\\S+", RegexOption.IGNORE_CASE)
        .findAll(this)
        .map { it.value.trimEnd('.', ',', ')', ']', '}', '>', '\"', '\'') }
        .firstOrNull { it.isSupportedPlaceReportUrl() }

fun String.extractSharedPlaceName(): String? =
    lineSequence()
        .map(String::trim)
        .firstOrNull { it.startsWith("[카카오맵]") || it.startsWith("[네이버지도]") }
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

private fun String.isSupportedPlaceReportHost(): Boolean =
    this == "kko.to" ||
        this == "naver.me" ||
        endsWith(".kakao.com") ||
        endsWith(".naver.com")
