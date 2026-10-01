package com.peto.ramap.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MyReviewNavigationSerializationTest {
    @Test
    fun `my reviews routes restore with the app serializer module`() {
        val json = Json { serializersModule = navKeySerializersModule() }
        val routes: List<NavKey> =
            listOf(
                ScreenRoutes.MyReviewsRoutes,
                ScreenRoutes.ReviewWriteRoutes("shop-id", "review-id"),
                ScreenRoutes.ReviewWriteRoutes("shop-id", null),
            )
        for (route in routes) {
            val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), route)
            assertEquals(route, json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded))
        }
    }
}
