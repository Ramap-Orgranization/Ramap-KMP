package com.peto.ramap.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop

class RamenShopPreviewParameterProvider : PreviewParameterProvider<RamenShop> {
    val ramenShopPreviewSamples =
        listOf(
            RamenShop(
                id = "1",
                kakaoPlaceId = null,
                name = "멘야 하나비",
                address = "서울 강남구 테헤란로 123",
                location = Location(lat = 37.5, lng = 127.0),
                kakaoPlaceUrl = null,
                phone = null,
                instagramUrl = null,
                menuCategories =
                    MenuCategories(
                        listOf(
                            Category.TONKOTSU,
                            Category.TSUKEMEN,
                        ),
                    ),
                isVisible = true,
                createdAt = "",
                updatedAt = "",
            ),
            RamenShop(
                id = "2",
                kakaoPlaceId = null,
                name = "라멘 트럭",
                address = "서울 마포구 독막로 14길 31",
                location = Location(lat = 37.55, lng = 126.92),
                kakaoPlaceUrl = null,
                phone = null,
                instagramUrl = null,
                menuCategories = MenuCategories(listOf(Category.SHOYU)),
                isVisible = true,
                createdAt = "",
                updatedAt = "",
            ),
        )

    override val values: Sequence<RamenShop> = ramenShopPreviewSamples.asSequence()
}
