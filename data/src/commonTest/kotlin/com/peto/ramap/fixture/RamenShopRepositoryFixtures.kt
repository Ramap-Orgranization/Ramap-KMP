package com.peto.ramap.fixture

import com.peto.ramap.data.model.RamenShopResponse

internal fun ramenShopResponseFixture(
    id: String,
    name: String = "라멘집",
    kakaoPlaceUrl: String? = "https://place.map.kakao.com/shop-1",
    instagramUrl: String? = "https://instagram.com/ramen_shop",
    menuCategoryIds: List<String>? = listOf("shoyu"),
): RamenShopResponse =
    RamenShopResponse(
        id = id,
        name = name,
        address = "서울시 마포구 라멘로 1",
        lat = 37.551,
        lng = 126.921,
        kakaoPlaceUrl = kakaoPlaceUrl,
        instagramUrl = instagramUrl,
        menuCategoryIds = menuCategoryIds,
        createdAt = "2026-06-01T00:00:00Z",
        updatedAt = "2026-06-02T00:00:00Z",
    )
