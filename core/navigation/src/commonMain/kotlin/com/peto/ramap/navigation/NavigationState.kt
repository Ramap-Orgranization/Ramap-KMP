package com.peto.ramap.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavigationState(
    selectedTabState: MutableState<TabStatus>,
    val backStacks: Map<TabStatus, NavBackStack<NavKey>>,
    private val onMapTabExited: () -> Unit = {},
) {
    var selectedTab: TabStatus by selectedTabState
        private set

    /** 마지막 네비게이션 소스. shop_select 등의 analytics source 파라미터로 사용. */
    var lastNavigationSource: NavigationSource? = null
        private set

    val currentBackStack: NavBackStack<NavKey>
        get() = backStacks.getValue(selectedTab)

    val currentRoute: ScreenRoutes
        get() = currentBackStack.last() as ScreenRoutes

    val canNavigateBack: Boolean
        get() =
            currentBackStack.size > 1 ||
                selectedTab != TabStatus.MAP ||
                requestedMapReturnTab() != null

    fun showHiddenShops() {
        if (currentRoute != ScreenRoutes.HiddenShopListRoutes) {
            currentBackStack.add(ScreenRoutes.HiddenShopListRoutes)
        }
    }

    fun showAccountSettings() = showOnce(ScreenRoutes.AccountSettingsRoutes)

    fun showInformation() = showOnce(ScreenRoutes.InformationRoutes)

    fun showPlaceReport() = showOnce(ScreenRoutes.PlaceReportRoutes)

    fun showNotificationSettings() {
        if (currentRoute != ScreenRoutes.NotificationSettingsRoutes) {
            currentBackStack.add(ScreenRoutes.NotificationSettingsRoutes)
        }
    }

    fun showSubscribedShops() = showOnce(ScreenRoutes.SubscribedShopListRoutes)

    fun showBookmarkedShops() = showOnce(ScreenRoutes.BookmarkedShopListRoutes)

    fun showImportation() = showOnce(ScreenRoutes.ImportationRoutes)

    fun showImportationGuide() = showOnce(ScreenRoutes.ImportationGuideRoutes)

    fun showEvent(eventId: String) {
        if (currentRoute == ScreenRoutes.EventDetailRoutes(eventId)) return
        currentBackStack.add(ScreenRoutes.EventDetailRoutes(eventId))
    }

    fun showEventRoot() {
        if (currentBackStack.lastOrNull() is ScreenRoutes.EventDetailRoutes) {
            currentBackStack.removeLastOrNull()
        }
        val eventBackStack = backStacks.getValue(TabStatus.EVENT)
        eventBackStack.clear()
        eventBackStack.add(ScreenRoutes.EventTabRoutes)
        selectTopLevelTab(TabStatus.EVENT)
    }

    fun pop() {
        if (currentBackStack.size > 1) {
            currentBackStack.removeLastOrNull()
            return
        }

        requestedMapReturnTab()?.let { returnTab ->
            selectTopLevelTab(returnTab)
            return
        }

        selectTopLevelTab(TabStatus.MAP)
    }

    fun selectTopLevelTab(tab: TabStatus) {
        if (tab == selectedTab) return

        if (selectedTab == TabStatus.MAP) {
            onMapTabExited()
            clearRequestedShopFromMapRoute()
        }
        selectedTab = tab
    }

    private fun clearRequestedShopFromMapRoute() {
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        val mapRoute = mapBackStack.firstOrNull() as? ScreenRoutes.TabRoutes ?: return
        if (mapRoute.shopId == null) return

        mapBackStack[0] = ScreenRoutes.TabRoutes()
    }

    fun showMap() {
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        mapBackStack.clear()
        mapBackStack.add(ScreenRoutes.TabRoutes())
        selectTopLevelTab(TabStatus.MAP)
    }

    fun showShopOnMap(
        shopId: String,
        source: NavigationSource? = null,
        returnTab: TabStatus? = null,
    ) {
        lastNavigationSource = source
        val mapRoute =
            ScreenRoutes.TabRoutes(
                shopId = shopId,
                returnTab = returnTab,
            )
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        val isRequestedShopAlreadyShown =
            selectedTab == TabStatus.MAP && mapBackStack.singleOrNull() == mapRoute
        if (isRequestedShopAlreadyShown) return

        mapBackStack.clear()
        mapBackStack.add(mapRoute)
        selectTopLevelTab(TabStatus.MAP)
    }

    fun consumeMapReturnOrigin() {
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        val route = mapBackStack.singleOrNull() as? ScreenRoutes.TabRoutes ?: return
        if (route.returnTab == null) return

        mapBackStack[0] = ScreenRoutes.TabRoutes()
    }

    private fun requestedMapReturnTab(): TabStatus? {
        if (selectedTab != TabStatus.MAP) return null
        return (currentBackStack.singleOrNull() as? ScreenRoutes.TabRoutes)?.returnTab
    }

    private fun showOnce(route: ScreenRoutes) {
        if (currentRoute != route) currentBackStack.add(route)
    }
}
