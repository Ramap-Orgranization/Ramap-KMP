package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.businesshour.BreakTime
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
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
                kakaoPlaceUrl = "https://place.map.kakao.com/123",
                naverPlaceUrl = "https://map.naver.com/v5/entry/place/123",
                phone = "02-123-4567",
                instagramUrl = "https://instagram.com/menyahana_bi",
                menuCategories = MenuCategories(listOf(Category.TONKOTSU, Category.TSUKEMEN)),
                isVisible = true,
                createdAt = "",
                updatedAt = "",
                businessHoursDetails =
                    BusinessHours(
                        weekly =
                            mapOf(
                                "mon" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                                "sun" to BusinessHoursDay(true, null, null, false, null),
                            ),
                        breakTimes = mapOf("mon" to listOf(BreakTime("14:00", "15:00"))),
                        lastOrders = emptyMap(),
                        notice = "재료 소진 시 조기 마감될 수 있습니다.",
                    ),
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
            RamenShop(
                id = "3",
                kakaoPlaceId = null,
                name = "하쿠텐 라멘",
                address = "서울 마포구 동교로 266-12",
                location = Location(lat = 37.56, lng = 126.92),
                kakaoPlaceUrl = null,
                phone = null,
                instagramUrl = null,
                menuCategories = MenuCategories(listOf(Category.IEKEI)),
                isVisible = true,
                createdAt = "",
                updatedAt = "",
            ),
            RamenShop(
                id = "4",
                kakaoPlaceId = null,
                name = "무타히로",
                address = "서울 마포구 포은로 11",
                location = Location(lat = 37.55, lng = 126.91),
                kakaoPlaceUrl = null,
                phone = null,
                instagramUrl = null,
                menuCategories = MenuCategories(listOf(Category.SHOYU, Category.NIBOSHI_GYOKAI)),
                isVisible = true,
                createdAt = "",
                updatedAt = "",
            ),
        )

    override val values: Sequence<RamenShop> = ramenShopPreviewSamples.asSequence()
}
