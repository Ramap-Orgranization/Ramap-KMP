package com.peto.ramap.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.peto.ramap.domain.model.event.ShopEvent

class NavigationState(
    val backStack: NavBackStack<NavKey>,
) {
    var selectedEvent by mutableStateOf<ShopEvent?>(null)
        private set

    var requestedMapShopId by mutableStateOf<String?>(null)
        private set

    val currentRoute: ScreenRoutes
        get() = backStack.last() as ScreenRoutes

    val canNavigateBack: Boolean
        get() = backStack.size > 1

    val selectedTab: TabStatus
        get() =
            when (currentRoute) {
                ScreenRoutes.TabRoutes -> TabStatus.MAP
                ScreenRoutes.EventTabRoutes -> TabStatus.EVENT
                ScreenRoutes.MyTabRoutes,
                ScreenRoutes.HiddenShopListRoutes,
                ScreenRoutes.NotificationSettingsRoutes,
                -> TabStatus.MY
                is ScreenRoutes.EventDetailRoutes ->
                    if (backStack.firstOrNull() == ScreenRoutes.EventTabRoutes) {
                        TabStatus.EVENT
                    } else {
                        TabStatus.MAP
                    }
            }

    fun showHiddenShops() {
        if (currentRoute != ScreenRoutes.HiddenShopListRoutes) {
            backStack.add(ScreenRoutes.HiddenShopListRoutes)
        }
    }

    fun showNotificationSettings() {
        if (currentRoute != ScreenRoutes.NotificationSettingsRoutes) {
            backStack.add(ScreenRoutes.NotificationSettingsRoutes)
        }
    }

    fun showEvent(event: ShopEvent) {
        selectedEvent = event
        showEvent(event.id)
    }

    fun showEvent(eventId: String) {
        if (currentRoute == ScreenRoutes.EventDetailRoutes(eventId)) return
        backStack.add(ScreenRoutes.EventDetailRoutes(eventId))
    }

    fun pop() {
        backStack.removeLastOrNull()
    }

    fun selectTopLevelTab(tab: TabStatus) {
        val rootRoute = tab.toRootRoute()
        if (currentRoute == rootRoute) return

        backStack.clear()
        backStack.add(rootRoute)
        selectedEvent = null
    }

    fun showMap() {
        selectTopLevelTab(TabStatus.MAP)
    }

    fun showShopOnMap(shopId: String) {
        requestedMapShopId = shopId
        showMap()
    }

    fun consumeMapShopRequest(shopId: String) {
        if (requestedMapShopId == shopId) {
            requestedMapShopId = null
        }
    }
}

private fun TabStatus.toRootRoute(): ScreenRoutes =
    when (this) {
        TabStatus.MAP -> ScreenRoutes.TabRoutes
        TabStatus.EVENT -> ScreenRoutes.EventTabRoutes
        TabStatus.MY -> ScreenRoutes.MyTabRoutes
    }
