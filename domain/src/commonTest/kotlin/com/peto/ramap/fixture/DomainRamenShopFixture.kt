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
    menuCategories: List<Category> = listOf(Category.SHOYU),
    isVisible: Boolean = true,
): RamenShop =
    RamenShop(
        id = id,
        kakaoPlaceId = "kakao-$id",
        name = name,
        address = address,
        location = location,
        kakaoPlaceUrl = null,
        phone = null,
        businessHours = null,
        instagramUrl = null,
        kakaoRating = null,
        menuCategories = MenuCategories(menuCategories),
        isVisible = isVisible,
        createdAt = "2026-06-01T00:00:00Z",
        updatedAt = "2026-06-02T00:00:00Z",
    )
