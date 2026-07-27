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
    fun `네 개 탭은 지도 랭킹 이벤트 설정 순서와 독립 스택을 가진다`() {
        val navigationState = navigationState()

        assertEquals(listOf(TabStatus.MAP, TabStatus.RANKING, TabStatus.EVENT, TabStatus.MY), TabStatus.entries)
        assertTrue(navigationState.backStacks.getValue(TabStatus.RANKING).isEmpty())

        navigationState.selectTopLevelTab(TabStatus.RANKING)
        assertEquals(ScreenRoutes.RankingTabRoutes, navigationState.currentRoute)
        navigationState.showShopOnMap("shop")

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(ScreenRoutes.TabRoutes("shop"), navigationState.currentRoute)
        assertTrue(navigationState.backStacks.getValue(TabStatus.RANKING).isEmpty())
    }

    @Test
    fun `랭킹 탭을 떠나면 스택을 비우고 재진입할 때 새 루트를 만든다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)
        val rankingBackStack = navigationState.backStacks.getValue(TabStatus.RANKING)

        assertEquals(listOf(ScreenRoutes.RankingTabRoutes), rankingBackStack.toList())

        navigationState.selectTopLevelTab(TabStatus.EVENT)

        assertTrue(rankingBackStack.isEmpty())

        navigationState.selectTopLevelTab(TabStatus.RANKING)

        assertEquals(listOf(ScreenRoutes.RankingTabRoutes), rankingBackStack.toList())
    }

    @Test
    fun `다른 탭으로 복원되면 남아 있는 랭킹 스택을 비운다`() {
        val rankingBackStack = NavBackStack<NavKey>(ScreenRoutes.RankingTabRoutes)

        navigationState(rankingBackStack = rankingBackStack)

        assertTrue(rankingBackStack.isEmpty())
    }

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
    fun `지도 루트를 열면 이전 매장 경로를 제거하고 지도 탭을 선택한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)
        navigationState.showShopOnMap("shop-id")
        navigationState.selectTopLevelTab(TabStatus.RANKING)

        navigationState.showMap()

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(
            listOf(ScreenRoutes.TabRoutes()),
            navigationState.backStacks.getValue(TabStatus.MAP).toList(),
        )
    }

    @Test
    fun `랭킹 매장을 지도에서 연 뒤 다른 탭을 거치면 매장 요청 없이 지도에 돌아온다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)

        navigationState.showShopOnMap("shop-id")
        assertEquals(ScreenRoutes.TabRoutes("shop-id"), navigationState.currentRoute)

        navigationState.selectTopLevelTab(TabStatus.EVENT)
        navigationState.selectTopLevelTab(TabStatus.MAP)

        assertEquals(ScreenRoutes.TabRoutes(), navigationState.currentRoute)
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

    private fun navigationState(
        selectedTab: TabStatus = TabStatus.MAP,
        rankingBackStack: NavBackStack<NavKey> = NavBackStack(),
    ): NavigationState =
        NavigationState(
            selectedTabState = mutableStateOf(selectedTab),
            backStacks =
                mapOf(
                    TabStatus.MAP to NavBackStack<NavKey>(ScreenRoutes.TabRoutes()),
                    TabStatus.RANKING to rankingBackStack,
                    TabStatus.EVENT to NavBackStack<NavKey>(ScreenRoutes.EventTabRoutes),
                    TabStatus.MY to NavBackStack<NavKey>(ScreenRoutes.MyTabRoutes),
                ),
        )
}
