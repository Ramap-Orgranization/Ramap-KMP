package com.peto.ramap.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavigationState(
    selectedTabState: MutableState<TabStatus>,
    val backStacks: Map<TabStatus, NavBackStack<NavKey>>,
    selectedNewsTabState: MutableState<NewsTab> = mutableStateOf(NewsTab.EVENTS),
) {
    var selectedNewsTab: NewsTab by selectedNewsTabState
        private set

    init {
        backStacks[TabStatus.MY]?.removeAll { it == ScreenRoutes.HiddenShopListRoutes }
        if (backStacks[TabStatus.EVENT]?.remove(ScreenRoutes.OperatingNoticeRoutes) == true) {
            selectedNewsTab = NewsTab.OPERATING_NOTICES
        }
    }

    private var eventTabRouteId = 0
    private var sharedShopRouteId =
        backStacks.values
            .flatten()
            .filterIsInstance<ScreenRoutes.MapRoutes>()
            .maxOfOrNull { it.instanceId } ?: 0

    var selectedTab: TabStatus by selectedTabState
        private set

    val currentBackStack: NavBackStack<NavKey>
        get() = backStacks.getValue(selectedTab)

    val currentRoute: ScreenRoutes
        get() = currentBackStack.last() as ScreenRoutes

    val canNavigateBack: Boolean
        get() =
            currentBackStack.size > 1 ||
                requestedMapReturnTab() != null ||
                selectedTab != TabStatus.MAP

    fun showMyRoot() {
        val myStack = backStacks.getValue(TabStatus.MY)
        myStack.clear()
        myStack.add(ScreenRoutes.MyTabRoutes)
        selectTopLevelTab(TabStatus.MY)
    }

    fun showReviewShopOnMap(shopId: String) {
        if (selectedTab == TabStatus.MAP) {
            showOnce(ScreenRoutes.MapRoutes(shopId = shopId, showShopDetail = true))
            return
        }
        val mapStack = backStacks.getValue(TabStatus.MAP)
        mapStack.clear()
        mapStack.add(ScreenRoutes.MapRoutes(shopId = shopId, returnTab = selectedTab, showShopDetail = true))
        // Preserve the originating review stack, including the event tab, for Back.
        selectedTab = TabStatus.MAP
    }

    fun showAnnouncements() = showOnce(ScreenRoutes.AnnouncementRoutes)

    fun showFollows() = showOnce(ScreenRoutes.FollowRoutes)

    fun showProfileEdit() = showOnce(ScreenRoutes.ProfileEditRoutes)

    fun showMyReviews() = showOnce(ScreenRoutes.MyReviewsRoutes)

    fun showOtherReviews(userId: String) {
        if (userId.isNotBlank()) showOnce(ScreenRoutes.OtherReviewsRoutes(userId))
    }

    fun showReviewDetail(reviewId: String) {
        if (reviewId.isNotBlank()) showOnce(ScreenRoutes.ReviewDetailRoutes(reviewId))
    }

    fun showReviewWrite(shopId: String) = showOnce(ScreenRoutes.ReviewWriteRoutes(shopId))

    fun showReviewShopSearch() = showOnce(ScreenRoutes.ReviewShopSearchRoutes)

    fun completeReviewWrite() {
        val route = currentRoute as? ScreenRoutes.ReviewWriteRoutes ?: return
        if (route.reviewId != null) {
            pop()
            return
        }
        if (currentBackStack.getOrNull(currentBackStack.lastIndex - 1) == ScreenRoutes.ReviewShopSearchRoutes) {
            currentBackStack.removeLastOrNull()
            currentBackStack.removeLastOrNull()
            return
        }
        showShopOnMap(shopId = route.shopId, showReviews = true)
    }

    fun showReviewEdit(
        shopId: String,
        reviewId: String,
    ) = showOnce(ScreenRoutes.ReviewWriteRoutes(shopId, reviewId))

    fun showSettings() = showOnce(ScreenRoutes.SettingsRoutes)

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

    fun showArchiveMap() = showOnce(ScreenRoutes.ArchiveMapRoutes)

    fun clearArchiveMap() {
        val mapStack = backStacks.getValue(TabStatus.MAP)
        val archiveIndex = mapStack.indexOf(ScreenRoutes.ArchiveMapRoutes)
        if (archiveIndex < 0) return
        while (mapStack.size > archiveIndex) mapStack.removeLastOrNull()
        if (mapStack.isEmpty()) mapStack.add(ScreenRoutes.MapRoutes())
    }

    fun selectNewsTab(tab: NewsTab) {
        selectedNewsTab = tab
    }

    fun showOperatingNotices(noticeId: String? = null) {
        val eventBackStack = backStacks.getValue(TabStatus.EVENT)
        eventBackStack.clear()
        eventBackStack.add(ScreenRoutes.EventTabRoutes(++eventTabRouteId, noticeId))
        selectTopLevelTab(TabStatus.EVENT)
        selectedNewsTab = NewsTab.OPERATING_NOTICES
    }

    fun showEventRoot() {
        if (
            currentBackStack.lastOrNull() is ScreenRoutes.EventDetailRoutes
        ) {
            currentBackStack.removeLastOrNull()
        }
        val eventBackStack = backStacks.getValue(TabStatus.EVENT)
        selectedNewsTab = NewsTab.EVENTS
        eventBackStack.clear()
        eventBackStack.add(ScreenRoutes.EventTabRoutes(++eventTabRouteId))
        selectTopLevelTab(TabStatus.EVENT)
    }

    fun pop() {
        if (currentBackStack.size > 1) {
            currentBackStack.removeLastOrNull()
            return
        }

        if (selectedTab == TabStatus.EVENT && selectedNewsTab == NewsTab.OPERATING_NOTICES) {
            selectedNewsTab = NewsTab.EVENTS
            return
        }

        requestedMapReturnTab()?.let { returnTab ->
            selectTopLevelTab(returnTab)
            return
        }

        selectBottomTab(TabStatus.MAP)
    }

    fun selectBottomTab(tab: TabStatus) {
        if (tab == selectedTab) return

        backStacks[TabStatus.FEED]?.clear()
        selectTopLevelTab(tab)
    }

    fun selectTopLevelTab(tab: TabStatus) {
        if (tab == selectedTab) return

        if (selectedTab == TabStatus.EVENT) {
            selectedNewsTab = NewsTab.EVENTS
            val eventBackStack = backStacks.getValue(TabStatus.EVENT)
            eventBackStack.clear()
            eventBackStack.add(ScreenRoutes.EventTabRoutes(++eventTabRouteId))
        }
        if (selectedTab == TabStatus.MAP) {
            clearRequestedShopFromMapRoute()
        }
        if (tab == TabStatus.FEED) {
            val feedBackStack = backStacks.getValue(TabStatus.FEED)
            if (feedBackStack.isEmpty()) feedBackStack.add(ScreenRoutes.FeedTabRoutes)
        }
        selectedTab = tab
    }

    private fun clearRequestedShopFromMapRoute() {
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        val mapRoute = mapBackStack.firstOrNull() as? ScreenRoutes.MapRoutes ?: return
        if (mapRoute.shopId == null) return

        mapBackStack[0] = ScreenRoutes.MapRoutes()
    }

    fun showMap() {
        val mapBackStack = backStacks.getValue(TabStatus.MAP)
        mapBackStack.clear()
        mapBackStack.add(ScreenRoutes.MapRoutes())
        selectTopLevelTab(TabStatus.MAP)
    }

    fun showShopOnMap(
        shopId: String,
        source: NavigationSource? = null,
        returnTab: TabStatus? = null,
        showShopDetail: Boolean = true,
        showReviews: Boolean = false,
    ) {
        val mapRoute =
            ScreenRoutes.MapRoutes(
                shopId = shopId,
                returnTab = returnTab,
                showShopDetail = showShopDetail,
                showReviews = showReviews,
                source = source,
                instanceId = if (source == NavigationSource.SHARED_LINK) ++sharedShopRouteId else 0,
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
        val route = mapBackStack.singleOrNull() as? ScreenRoutes.MapRoutes ?: return
        if (route.returnTab == null) return

        mapBackStack[0] = ScreenRoutes.MapRoutes()
    }

    private fun requestedMapReturnTab(): TabStatus? {
        if (selectedTab != TabStatus.MAP) return null
        return (currentBackStack.singleOrNull() as? ScreenRoutes.MapRoutes)?.returnTab
    }

    private fun showOnce(route: ScreenRoutes) {
        if (currentRoute != route) currentBackStack.add(route)
    }
}
