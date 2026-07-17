package com.peto.ramap.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavigationState(
    selectedTabState: MutableState<TabStatus>,
    val backStacks: Map<TabStatus, NavBackStack<NavKey>>,
) {
    var selectedTab: TabStatus by selectedTabState
        private set

    val currentBackStack: NavBackStack<NavKey>
        get() = backStacks.getValue(selectedTab)

    val currentRoute: ScreenRoutes
        get() = currentBackStack.last() as ScreenRoutes

    val canNavigateBack: Boolean
        get() = currentBackStack.size > 1 || selectedTab != TabStatus.MAP

    fun showHiddenShops() {
        if (currentRoute != ScreenRoutes.HiddenShopListRoutes) {
            currentBackStack.add(ScreenRoutes.HiddenShopListRoutes)
        }
    }

    fun showNotificationSettings() {
        if (currentRoute != ScreenRoutes.NotificationSettingsRoutes) {
            currentBackStack.add(ScreenRoutes.NotificationSettingsRoutes)
        }
    }

    fun showEvent(eventId: String) {
        if (currentRoute == ScreenRoutes.EventDetailRoutes(eventId)) return
        currentBackStack.add(ScreenRoutes.EventDetailRoutes(eventId))
    }

    fun pop() {
        if (currentBackStack.size > 1) {
            currentBackStack.removeLastOrNull()
            return
        }

        selectedTab = TabStatus.MAP
    }

    fun selectTopLevelTab(tab: TabStatus) {
        selectedTab = tab
    }

    fun showMap() {
        selectTopLevelTab(TabStatus.MAP)
    }

    fun showShopOnMap(shopId: String) {
        val mapRoute = ScreenRoutes.TabRoutes(shopId = shopId)
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        val isRequestedShopAlreadyShown =
            selectedTab == TabStatus.MAP && mapBackStack.singleOrNull() == mapRoute
        if (isRequestedShopAlreadyShown) return

        mapBackStack.clear()
        mapBackStack.add(mapRoute)
        selectedTab = TabStatus.MAP
    }
}
