package com.peto.ramap.domain.model.report

enum class ShopInformationField(
    val key: String,
) {
    ADDRESS("address"),
    BUSINESS_HOURS("business_hours"),
    MENU_CATEGORIES("menu_categories"),
    WAITING("waiting"),
    INSTAGRAM("instagram"),
    KAKAO_MAP("kakao_map"),
    NAVER_MAP("naver_map"),
    OTHER("other"),
}
