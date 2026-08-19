package com.peto.ramap.data.datasource.shop

internal data class ShopTextSearchFilter(
    val pattern: String,
    val columns: List<String> = listOf("name", "address", "phone", "business_hours_notice", "kakao_place_id", "naver_place_url"),
)
