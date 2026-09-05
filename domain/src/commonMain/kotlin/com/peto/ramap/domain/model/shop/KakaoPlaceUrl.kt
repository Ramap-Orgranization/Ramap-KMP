package com.peto.ramap.domain.model.shop

object KakaoPlaceUrl {
    fun extractPlaceId(url: String?): String? =
        url
            ?.trim()
            ?.let(PLACE_ID_PATTERN::find)
            ?.groupValues
            ?.getOrNull(1)

    private val PLACE_ID_PATTERN =
        Regex("^https?://place\\.map\\.kakao\\.com/(\\d+)(?:[/?#]|$)", RegexOption.IGNORE_CASE)
}
