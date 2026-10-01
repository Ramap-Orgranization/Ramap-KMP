package com.peto.ramap.fixture

import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop

fun ramenShopFixture(
    id: String = "shop-1",
    name: String = "라멘집",
    address: String = "서울시 마포구 라멘로 1",
    location: Location = Location(lat = 37.551, lng = 126.921),
    kakaoPlaceUrl: String? = "https://place.map.kakao.com/shop-1",
    instagramUrl: String? = "https://instagram.com/ramen_shop",
    menuCategories: List<Category> = listOf(Category.SHOYU),
    isVisible: Boolean = true,
    createdAt: String = "2026-06-01T00:00:00Z",
    updatedAt: String = "2026-06-02T00:00:00Z",
): RamenShop =
    RamenShop(
        id = id,
        name = name,
        address = address,
        location = location,
        kakaoPlaceUrl = kakaoPlaceUrl,
        instagramUrl = instagramUrl,
        menuCategories = MenuCategories(menuCategories),
        isVisible = isVisible,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
