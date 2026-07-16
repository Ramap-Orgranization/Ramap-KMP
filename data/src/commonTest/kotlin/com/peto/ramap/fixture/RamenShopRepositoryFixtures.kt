package com.peto.ramap.fixture

import com.peto.ramap.data.model.RamenShopResponse

fun ramenShopResponseFixture(
    id: String,
    kakaoPlaceId: String? = "kakao-shop-1",
    name: String = "라멘집",
    kakaoPlaceUrl: String? = "https://place.map.kakao.com/shop-1",
    phone: String? = "02-0000-0000",
    businessHours: String? = "11:00-21:00",
    instagramUrl: String? = "https://instagram.com/ramen_shop",
    kakaoRating: Double? = 4.5,
    menuCategoryIds: List<String>? = listOf("shoyu"),
    isVisible: Boolean? = true,
): RamenShopResponse =
    RamenShopResponse(
        id = id,
        kakaoPlaceId = kakaoPlaceId,
        name = name,
        address = "서울시 마포구 라멘로 1",
        lat = 37.551,
        lng = 126.921,
        kakaoPlaceUrl = kakaoPlaceUrl,
        phone = phone,
        businessHours = businessHours,
        instagramUrl = instagramUrl,
        kakaoRating = kakaoRating,
        menuCategoryIds = menuCategoryIds,
        isVisible = isVisible,
        createdAt = "2026-06-01T00:00:00Z",
        updatedAt = "2026-06-02T00:00:00Z",
    )
