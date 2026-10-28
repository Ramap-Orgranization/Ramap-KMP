package com.peto.ramap.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveMapNavigationSerializationTest {
    @Test
    fun `매장 찾기와 리뷰 작성 경로는 앱 직렬화 모듈로 복원한다`() {
        val json = Json { serializersModule = navKeySerializersModule() }
        for (route in listOf(ScreenRoutes.ReviewShopSearchRoutes, ScreenRoutes.ReviewWriteRoutes("shop"))) {
            val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), route)
            assertEquals(route, json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded))
        }
    }

    @Test
    fun `아카이브 경로는 앱 직렬화 모듈로 복원한다`() {
        val json = Json { serializersModule = navKeySerializersModule() }
        val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), ScreenRoutes.ArchiveMapRoutes)
        assertEquals(ScreenRoutes.ArchiveMapRoutes, json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded))
    }
}
