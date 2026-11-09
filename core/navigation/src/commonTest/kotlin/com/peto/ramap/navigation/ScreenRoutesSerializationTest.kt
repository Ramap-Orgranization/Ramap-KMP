package com.peto.ramap.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenRoutesSerializationTest {
    @Test
    fun `모든 ScreenRoutes 서브클래스는 navKeySerializersModule로 직렬화 및 복원 가능하다`() {
        val json = Json { serializersModule = navKeySerializersModule() }
        val allRoutes: List<ScreenRoutes> =
            listOf(
                ScreenRoutes.MapRoutes(),
                ScreenRoutes.ArchiveMapRoutes,
                ScreenRoutes.ReviewShopSearchRoutes,
                ScreenRoutes.EventTabRoutes(),
                ScreenRoutes.OperatingNoticeRoutes,
                ScreenRoutes.RankingTabRoutes,
                ScreenRoutes.FeedTabRoutes,
                ScreenRoutes.MyTabRoutes,
                ScreenRoutes.SettingsRoutes,
                ScreenRoutes.AccountSettingsRoutes,
                ScreenRoutes.ProfileEditRoutes,
                ScreenRoutes.AnnouncementRoutes,
                ScreenRoutes.FollowRoutes,
                ScreenRoutes.MyReviewsRoutes,
                ScreenRoutes.OtherReviewsRoutes("user-1"),
                ScreenRoutes.ReviewWriteRoutes("shop-1"),
                ScreenRoutes.InformationRoutes,
                ScreenRoutes.PlaceReportRoutes,
                ScreenRoutes.HiddenShopListRoutes,
                ScreenRoutes.NotificationSettingsRoutes,
                ScreenRoutes.SubscribedShopListRoutes,
                ScreenRoutes.BookmarkedShopListRoutes,
                ScreenRoutes.ImportationRoutes,
                ScreenRoutes.ImportationGuideRoutes,
                ScreenRoutes.EventDetailRoutes("event-1"),
            )

        for (route in allRoutes) {
            val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), route)
            val decoded = json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded)
            assertEquals(route, decoded, "Failed to serialize/deserialize $route")
        }
    }
}
