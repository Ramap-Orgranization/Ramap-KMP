package com.peto.ramap.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface ScreenRoutes : NavKey {
    @Serializable
    data object TabRoutes : ScreenRoutes

    @Serializable
    data object EventTabRoutes : ScreenRoutes

    @Serializable
    data object MyTabRoutes : ScreenRoutes

    @Serializable
    data object HiddenShopListRoutes : ScreenRoutes

    @Serializable
    data object NotificationSettingsRoutes : ScreenRoutes

    @Serializable
    data class EventDetailRoutes(
        val eventId: String,
    ) : ScreenRoutes
}
