package com.peto.ramap.data.model

import com.peto.ramap.fixture.ramenShopResponseFixture
import com.peto.ramap.shared.RamapConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class RamenShopResponseTest {
    @Test
    fun `프로필 이미지 경로를 Storage 공개 URL로 변환한다`() {
        val response =
            ramenShopResponseFixture(id = "shop-1").copy(
                instagramProfileImagePath = "shop-1/profile.jpg",
            )

        val shop = response.toDomain()

        assertEquals(
            "${RamapConfig.SUPABASE_URL}/storage/v1/object/public/shop-profile-images/shop-1/profile.jpg",
            shop.instagramProfileImageUrl,
        )
    }
}
