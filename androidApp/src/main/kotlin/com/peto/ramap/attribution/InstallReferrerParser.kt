package com.peto.ramap.attribution

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Google Play Install Referrer 문자열에서 유입 경로 정보를 추출한다.
 *
 * 지원하는 쿼리 파라미터는 `click_id`, `source`, `campaign`, `shop_id`이며,
 * URL 디코딩에 실패하거나 값이 비어 있거나 최대 길이를 초과한 항목은 제외한다.
 */
internal object InstallReferrerParser {
    /**
     * Play Install Referrer 쿼리를 파싱해 유효한 유입 경로 정보만 반환한다.
     *
     * @param referrer Play Store가 전달한 Install Referrer 원본 쿼리 문자열
     * @return 추출한 유입 경로 정보. 유효한 항목이 없으면 비어 있는 객체를 반환한다.
     */
    fun parse(referrer: String?): InstallReferrerAttribution {
        val query = referrer?.trim()?.removePrefix(QUERY_PREFIX).orEmpty()
        if (query.isBlank()) return InstallReferrerAttribution()

        val values = query.split(QUERY_PARAMETER_SEPARATOR).mapNotNull(::parsePair).toMap()
        return InstallReferrerAttribution(
            clickId = sanitizeValue(values[CLICK_ID_PARAMETER]),
            source = sanitizeValue(values[SOURCE_PARAMETER]),
            campaign = sanitizeValue(values[CAMPAIGN_PARAMETER]),
            shopId = sanitizeValue(values[SHOP_ID_PARAMETER]),
        )
    }

    private fun parsePair(pair: String): Pair<String, String>? {
        val separator = pair.indexOf(KEY_VALUE_SEPARATOR)
        if (separator <= 0) return null
        return runCatching {
            URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8.name())
        }.getOrNull()
    }

    private fun sanitizeValue(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_VALUE_LENGTH }

    private const val QUERY_PREFIX = "?"
    private const val QUERY_PARAMETER_SEPARATOR = '&'
    private const val KEY_VALUE_SEPARATOR = '='

    private const val CLICK_ID_PARAMETER = "click_id"
    private const val SOURCE_PARAMETER = "source"
    private const val CAMPAIGN_PARAMETER = "campaign"
    private const val SHOP_ID_PARAMETER = "shop_id"

    private const val MAX_VALUE_LENGTH = 200
}
