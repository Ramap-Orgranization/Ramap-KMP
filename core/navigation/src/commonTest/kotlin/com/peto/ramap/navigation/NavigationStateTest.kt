package com.peto.ramap.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationStateTest {
    @Test
    fun `탭을 전환해도 각 탭의 중첩 경로를 유지한다`() {
        val navigationState = navigationState()

        navigationState.selectTopLevelTab(TabStatus.EVENT)
        navigationState.showEvent("event-id")
        navigationState.selectTopLevelTab(TabStatus.MY)
        navigationState.showNotificationSettings()
        navigationState.selectTopLevelTab(TabStatus.EVENT)

        assertEquals(ScreenRoutes.EventDetailRoutes("event-id"), navigationState.currentRoute)
        assertEquals(
            listOf(ScreenRoutes.EventTabRoutes, ScreenRoutes.EventDetailRoutes("event-id")),
            navigationState.backStacks.getValue(TabStatus.EVENT).toList(),
        )

        navigationState.selectTopLevelTab(TabStatus.MY)

        assertEquals(ScreenRoutes.NotificationSettingsRoutes, navigationState.currentRoute)
    }

    @Test
    fun `뒤로 가면 현재 탭의 중첩 경로를 제거한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.EVENT)
        navigationState.showEvent("event-id")

        navigationState.pop()

        assertEquals(TabStatus.EVENT, navigationState.selectedTab)
        assertEquals(ScreenRoutes.EventTabRoutes, navigationState.currentRoute)
        assertTrue(navigationState.canNavigateBack)
    }

    @Test
    fun `지도 외 탭의 루트에서 뒤로 가면 유지된 지도 스택으로 돌아간다`() {
        val navigationState = navigationState()
        navigationState.showEvent("event-id")
        navigationState.selectTopLevelTab(TabStatus.EVENT)

        navigationState.pop()

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(ScreenRoutes.EventDetailRoutes("event-id"), navigationState.currentRoute)
        assertTrue(navigationState.canNavigateBack)
    }

    @Test
    fun `지도 탭 루트에서는 뒤로 갈 수 없다`() {
        val navigationState = navigationState()

        assertFalse(navigationState.canNavigateBack)

        navigationState.pop()

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(ScreenRoutes.TabRoutes(), navigationState.currentRoute)
    }

    @Test
    fun `이벤트를 열면 현재 탭 스택에 상세 경로를 추가한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.EVENT)

        navigationState.showEvent("event-id")

        assertEquals(ScreenRoutes.EventDetailRoutes("event-id"), navigationState.currentRoute)
        assertEquals(1, navigationState.backStacks.getValue(TabStatus.MAP).size)
        assertEquals(2, navigationState.backStacks.getValue(TabStatus.EVENT).size)
    }

    @Test
    fun `지도에서 매장을 열면 지도 스택만 교체하고 지도 탭을 선택한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.EVENT)
        navigationState.showEvent("event-id")

        navigationState.showShopOnMap("shop-id")

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(ScreenRoutes.TabRoutes("shop-id"), navigationState.currentRoute)
        assertEquals(1, navigationState.currentBackStack.size)
        assertEquals(
            ScreenRoutes.EventDetailRoutes("event-id"),
            navigationState.backStacks.getValue(TabStatus.EVENT).last(),
        )
    }

    @Test
    fun `현재 탭을 다시 선택해도 해당 탭 스택을 유지한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.EVENT)
        navigationState.showEvent("event-id")

        navigationState.selectTopLevelTab(TabStatus.EVENT)

        assertEquals(ScreenRoutes.EventDetailRoutes("event-id"), navigationState.currentRoute)
    }

    @Test
    fun `설정 목록의 모든 하위 화면을 전체 화면 경로로 연다`() {
        val navigationState = navigationState(selectedTab = TabStatus.MY)

        navigationState.showAccountSettings()
        assertEquals(ScreenRoutes.AccountSettingsRoutes, navigationState.currentRoute)
        navigationState.pop()
        navigationState.showInformation()
        assertEquals(ScreenRoutes.InformationRoutes, navigationState.currentRoute)
        navigationState.pop()
        navigationState.showPlaceReport()
        assertEquals(ScreenRoutes.PlaceReportRoutes, navigationState.currentRoute)
        navigationState.pop()
        navigationState.showSubscribedShops()
        assertEquals(ScreenRoutes.SubscribedShopListRoutes, navigationState.currentRoute)
        navigationState.pop()
        navigationState.showBookmarkedShops()
        assertEquals(ScreenRoutes.BookmarkedShopListRoutes, navigationState.currentRoute)
    }

    private fun navigationState(selectedTab: TabStatus = TabStatus.MAP): NavigationState =
        NavigationState(
            selectedTabState = mutableStateOf(selectedTab),
            backStacks =
                mapOf(
                    TabStatus.MAP to NavBackStack<NavKey>(ScreenRoutes.TabRoutes()),
                    TabStatus.EVENT to NavBackStack<NavKey>(ScreenRoutes.EventTabRoutes),
                    TabStatus.MY to NavBackStack<NavKey>(ScreenRoutes.MyTabRoutes),
                ),
        )
}
