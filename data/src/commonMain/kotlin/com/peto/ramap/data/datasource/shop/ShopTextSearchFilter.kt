package com.peto.ramap.data.datasource.shop

internal data class ShopTextSearchFilter(
    val pattern: String,
    val columns: List<String> = listOf("name", "address", "business_hours_notice", "kakao_place_url", "naver_place_url"),
)
