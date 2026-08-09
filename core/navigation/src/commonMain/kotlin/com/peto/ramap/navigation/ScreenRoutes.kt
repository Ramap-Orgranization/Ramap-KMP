package com.peto.ramap.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface ScreenRoutes : NavKey {
    @Serializable
    data class TabRoutes(
        val shopId: String? = null,
        val returnTab: TabStatus? = null,
    ) : ScreenRoutes

    @Serializable
    data object EventTabRoutes : ScreenRoutes

    @Serializable
    data object RankingTabRoutes : ScreenRoutes

    @Serializable
    data object MyTabRoutes : ScreenRoutes

    @Serializable
    data object AccountSettingsRoutes : ScreenRoutes

    @Serializable
    data object InformationRoutes : ScreenRoutes

    @Serializable
    data object PlaceReportRoutes : ScreenRoutes

    @Serializable
    data object HiddenShopListRoutes : ScreenRoutes

    @Serializable
    data object NotificationSettingsRoutes : ScreenRoutes

    @Serializable
    data object SubscribedShopListRoutes : ScreenRoutes

    @Serializable
    data object BookmarkedShopListRoutes : ScreenRoutes

    @Serializable
    data object ImportationRoutes : ScreenRoutes

    @Serializable
    data class EventDetailRoutes(
        val eventId: String,
    ) : ScreenRoutes
}
