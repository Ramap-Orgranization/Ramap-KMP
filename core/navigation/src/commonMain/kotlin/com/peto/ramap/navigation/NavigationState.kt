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

    /** 마지막 네비게이션 소스. shop_select 등의 analytics source 파라미터로 사용. */
    var lastNavigationSource: String? = null
        private set

    init {
        val rankingBackStack = backStacks.getValue(TabStatus.RANKING)
        if (selectedTab == TabStatus.RANKING && rankingBackStack.isEmpty()) {
            rankingBackStack.add(ScreenRoutes.RankingTabRoutes)
        } else if (selectedTab != TabStatus.RANKING) {
            rankingBackStack.clear()
        }
    }

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

    fun showEvent(eventId: String) {
        if (currentRoute == ScreenRoutes.EventDetailRoutes(eventId)) return
        currentBackStack.add(ScreenRoutes.EventDetailRoutes(eventId))
    }

    fun pop() {
        if (currentBackStack.size > 1) {
            currentBackStack.removeLastOrNull()
            return
        }

        selectTopLevelTab(TabStatus.MAP)
    }

    fun selectTopLevelTab(tab: TabStatus) {
        if (tab == selectedTab) return

        if (selectedTab == TabStatus.RANKING) {
            backStacks.getValue(TabStatus.RANKING).clear()
        }
        if (tab == TabStatus.RANKING) {
            val rankingBackStack = backStacks.getValue(TabStatus.RANKING)
            rankingBackStack.clear()
            rankingBackStack.add(ScreenRoutes.RankingTabRoutes)
        }
        selectedTab = tab
    }

    fun showMap() {
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        mapBackStack.clear()
        mapBackStack.add(ScreenRoutes.TabRoutes())
        selectTopLevelTab(TabStatus.MAP)
    }

    fun showShopOnMap(
        shopId: String,
        source: String? = null,
    ) {
        lastNavigationSource = source
        val mapRoute = ScreenRoutes.TabRoutes(shopId = shopId)
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        val isRequestedShopAlreadyShown =
            selectedTab == TabStatus.MAP && mapBackStack.singleOrNull() == mapRoute
        if (isRequestedShopAlreadyShown) return

        mapBackStack.clear()
        mapBackStack.add(mapRoute)
        selectTopLevelTab(TabStatus.MAP)
    }

    private fun showOnce(route: ScreenRoutes) {
        if (currentRoute != route) currentBackStack.add(route)
    }
}
