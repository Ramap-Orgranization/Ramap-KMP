package com.peto.ramap.data.model

import com.peto.ramap.network.config.RamapSecrets
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProfileVisitRankingResponseTest {
    @Test
    fun `매장 사진을 공개 URL로 변환하고 사진 필드가 없는 응답도 읽는다`() {
        val response =
            Json.decodeFromString<ProfileVisitRankingResponse>(
                """
                {
                    "available": true,
                    "shops": [
                        {
                            "shop_id": "shop-1",
                            "shop_name": "라멘집",
                            "visit_count": 3,
                            "instagram_profile_image_path": "shop-1/profile.jpg"
                        },
                        {
                            "shop_id": "shop-2",
                            "shop_name": "다른 라멘집",
                            "visit_count": 1
                        }
                    ]
                }
                """.trimIndent(),
            )

        val shops = assertNotNull(response.toDomain()).values
        assertEquals(
            RamapSecrets.r2PublicBaseUrl
                .trimEnd('/')
                .takeIf(String::isNotBlank)
                ?.let { "$it/shop-profile-images/shop-1/profile.jpg" },
            shops[0].instagramProfileImageUrl,
        )
        assertNull(shops[1].instagramProfileImageUrl)
    }

    @Test
    fun `응답에 같은 방문 횟수의 매장이 없어도 서버가 동률로 표시한 매장은 순위를 부여하지 않는다`() {
        val response =
            Json.decodeFromString<ProfileVisitRankingResponse>(
                """
                {
                    "available": true,
                    "shops": [
                        {"shop_id": "shop-1", "shop_name": "라멘집", "visit_count": 2},
                        {"shop_id": "shop-2", "shop_name": "다른 라멘집", "visit_count": 1, "is_tied": true}
                    ]
                }
                """.trimIndent(),
            )

        val ranking = assertNotNull(response.toDomain())
        assertEquals(1, ranking.rankAt(0))
        assertNull(ranking.rankAt(1))
    }
}
