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
    fun `아카이브에서 매장을 찾아 첫 리뷰를 저장하면 아카이브로 돌아간다`() {
        val state = navigationState()
        state.showArchiveMap()
        state.showReviewShopSearch()
        state.showReviewWrite("shop")

        state.completeReviewWrite()

        assertEquals(TabStatus.MAP, state.selectedTab)
        assertEquals(listOf(ScreenRoutes.MapRoutes(), ScreenRoutes.ArchiveMapRoutes), state.currentBackStack.toList())
    }

    @Test
    fun `피드에서 매장을 찾아 리뷰를 저장하면 피드로 돌아간다`() {
        val state = navigationState(selectedTab = TabStatus.FEED)
        state.showReviewShopSearch()
        state.showReviewWrite("shop")

        state.completeReviewWrite()

        assertEquals(TabStatus.FEED, state.selectedTab)
        assertEquals(listOf(ScreenRoutes.FeedTabRoutes), state.currentBackStack.toList())
    }

    @Test
    fun `작성을 취소하면 매장 찾기로 돌아가고 찾기를 닫으면 출발 화면이 유지된다`() {
        val state = navigationState()
        state.showArchiveMap()
        state.showReviewShopSearch()
        state.showReviewWrite("shop")

        state.pop()
        assertEquals(ScreenRoutes.ReviewShopSearchRoutes, state.currentRoute)
        state.pop()
        assertEquals(ScreenRoutes.ArchiveMapRoutes, state.currentRoute)
    }

    @Test
    fun `기존 경로의 작성 완료는 지도 리뷰로 이동하고 수정 완료는 출발 화면으로 돌아간다`() {
        val state = navigationState(selectedTab = TabStatus.MY)
        state.showReviewWrite("shop")
        state.completeReviewWrite()
        assertEquals(ScreenRoutes.MapRoutes(shopId = "shop", showReviews = true), state.currentRoute)

        state.showReviewEdit("shop", "review")
        state.completeReviewWrite()
        assertEquals(ScreenRoutes.MapRoutes(shopId = "shop", showReviews = true), state.currentRoute)
    }

    @Test
    fun `리뷰 작성은 현재 탭에서 한 단계로 열리고 뒤로 가면 출발 화면으로 돌아간다`() {
        for (tab in TabStatus.entries) {
            val state = navigationState(selectedTab = tab)
            val sourceRoute = state.currentRoute
            val sourceStackSize = state.currentBackStack.size

            state.showReviewWrite("shop")

            assertEquals(ScreenRoutes.ReviewWriteRoutes("shop"), state.currentRoute)
            assertEquals(sourceStackSize + 1, state.currentBackStack.size)

            state.pop()

            assertEquals(tab, state.selectedTab)
            assertEquals(sourceRoute, state.currentRoute)
            assertEquals(sourceStackSize, state.currentBackStack.size)
        }
    }

    @Test
    fun `리뷰 작성 중 로그인하면 마이 탭 스택을 초기화하고 루트로 이동한다`() {
        for (tab in TabStatus.entries) {
            val state = navigationState(selectedTab = TabStatus.MY)
            state.showSettings()
            state.selectTopLevelTab(tab)
            state.showReviewWrite("shop")

            state.showMyRoot()

            assertEquals(TabStatus.MY, state.selectedTab)
            assertEquals(listOf(ScreenRoutes.MyTabRoutes), state.backStacks.getValue(TabStatus.MY).toList())
        }
    }

    @Test
    fun `다섯 개 탭은 지도 랭킹 이벤트 피드 마이 순서와 독립 스택을 가진다`() {
        val navigationState = navigationState()

        assertEquals(listOf(TabStatus.MAP, TabStatus.RANKING, TabStatus.EVENT, TabStatus.FEED, TabStatus.MY), TabStatus.entries)
        assertEquals(
            listOf(ScreenRoutes.RankingTabRoutes),
            navigationState.backStacks.getValue(TabStatus.RANKING).toList(),
        )

        navigationState.selectTopLevelTab(TabStatus.RANKING)
        assertEquals(ScreenRoutes.RankingTabRoutes, navigationState.currentRoute)
        navigationState.showShopOnMap("shop")

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(ScreenRoutes.MapRoutes("shop"), navigationState.currentRoute)
        assertEquals(
            listOf(ScreenRoutes.RankingTabRoutes),
            navigationState.backStacks.getValue(TabStatus.RANKING).toList(),
        )
    }

    @Test
    fun `랭킹 탭을 떠나도 스택을 유지한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)
        val rankingBackStack = navigationState.backStacks.getValue(TabStatus.RANKING)

        assertEquals(listOf(ScreenRoutes.RankingTabRoutes), rankingBackStack.toList())

        navigationState.selectTopLevelTab(TabStatus.EVENT)

        assertEquals(listOf(ScreenRoutes.RankingTabRoutes), rankingBackStack.toList())

        navigationState.selectTopLevelTab(TabStatus.RANKING)

        assertEquals(listOf(ScreenRoutes.RankingTabRoutes), rankingBackStack.toList())
    }

    @Test
    fun `다른 탭으로 복원돼도 랭킹 스택을 유지한다`() {
        val rankingBackStack = NavBackStack<NavKey>(ScreenRoutes.RankingTabRoutes)

        navigationState(rankingBackStack = rankingBackStack)

        assertEquals(listOf(ScreenRoutes.RankingTabRoutes), rankingBackStack.toList())
    }

    @Test
    fun `이벤트 탭을 떠나면 이벤트 스택을 초기화하고 다른 탭 스택은 유지한다`() {
        val navigationState = navigationState()

        navigationState.selectTopLevelTab(TabStatus.EVENT)
        navigationState.showEvent("event-id")
        navigationState.selectTopLevelTab(TabStatus.MY)
        navigationState.showNotificationSettings()
        navigationState.selectTopLevelTab(TabStatus.EVENT)

        assertEquals(
            listOf(ScreenRoutes.EventTabRoutes(1)),
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
        assertEquals(ScreenRoutes.EventTabRoutes(), navigationState.currentRoute)
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
        assertEquals(ScreenRoutes.MapRoutes(), navigationState.currentRoute)
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
    fun `이용 불가 이벤트에서 이벤트 루트로 이동하면 실패한 상세 경로를 제거한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.EVENT)
        navigationState.showEvent("missing-event")

        navigationState.showEventRoot()

        assertEquals(TabStatus.EVENT, navigationState.selectedTab)
        assertEquals(
            listOf(ScreenRoutes.EventTabRoutes(1)),
            navigationState.backStacks.getValue(TabStatus.EVENT).toList(),
        )
    }

    @Test
    fun `다른 탭에서 연 이용 불가 이벤트는 소유 스택에서도 제거한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)
        navigationState.showEvent("missing-event")

        navigationState.showEventRoot()
        navigationState.selectTopLevelTab(TabStatus.RANKING)

        assertEquals(ScreenRoutes.RankingTabRoutes, navigationState.currentRoute)
        assertEquals(
            listOf(ScreenRoutes.RankingTabRoutes),
            navigationState.backStacks.getValue(TabStatus.RANKING).toList(),
        )
    }

    @Test
    fun `이벤트에서 매장을 열면 지도 스택을 교체하고 이벤트 스택을 초기화한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.EVENT)
        navigationState.showEvent("event-id")

        navigationState.showShopOnMap("shop-id")

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(ScreenRoutes.MapRoutes("shop-id"), navigationState.currentRoute)
        assertEquals(1, navigationState.currentBackStack.size)
        assertEquals(
            ScreenRoutes.EventTabRoutes(1),
            navigationState.backStacks.getValue(TabStatus.EVENT).last(),
        )
    }

    @Test
    fun `매장을 지도에서 열 때 네비게이션 출처를 저장한다`() {
        val navigationState = navigationState()

        navigationState.showShopOnMap(
            shopId = "shop-id",
            source = NavigationSource.RANKING,
        )

        assertEquals(
            ScreenRoutes.MapRoutes(
                shopId = "shop-id",
                source = NavigationSource.RANKING,
            ),
            navigationState.currentRoute,
        )
        assertEquals("ranking", NavigationSource.RANKING.value)
    }

    @Test
    fun `지도 루트를 열면 이전 매장 경로를 제거하고 지도 탭을 선택한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)
        navigationState.showShopOnMap("shop-id")
        navigationState.selectTopLevelTab(TabStatus.RANKING)

        navigationState.showMap()

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(
            listOf(ScreenRoutes.MapRoutes()),
            navigationState.backStacks.getValue(TabStatus.MAP).toList(),
        )
    }

    @Test
    fun `지도 탭 선택과 뒤로 가기로 떠나면 요청한 매장 경로를 정리한다`() {
        val navigationState = navigationState()
        navigationState.showShopOnMap("shop-id")

        navigationState.selectTopLevelTab(TabStatus.EVENT)
        assertEquals(TabStatus.EVENT, navigationState.selectedTab)
        assertEquals(
            listOf(ScreenRoutes.MapRoutes()),
            navigationState.backStacks.getValue(TabStatus.MAP).toList(),
        )

        navigationState.selectTopLevelTab(TabStatus.MAP)
        navigationState.showShopOnMap(
            shopId = "shop-id",
            returnTab = TabStatus.EVENT,
        )
        navigationState.pop()

        assertEquals(TabStatus.EVENT, navigationState.selectedTab)
        assertEquals(
            listOf(ScreenRoutes.MapRoutes()),
            navigationState.backStacks.getValue(TabStatus.MAP).toList(),
        )
    }

    @Test
    fun `현재 지도 탭을 다시 선택하면 요청한 매장 경로를 유지한다`() {
        val navigationState = navigationState()
        navigationState.showShopOnMap("shop-id")

        navigationState.selectTopLevelTab(TabStatus.MAP)

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(ScreenRoutes.MapRoutes("shop-id"), navigationState.currentRoute)
    }

    @Test
    fun `랭킹 매장을 지도에서 연 뒤 다른 탭을 거치면 매장 요청 없이 지도에 돌아온다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)

        navigationState.showShopOnMap("shop-id")
        assertEquals(ScreenRoutes.MapRoutes("shop-id"), navigationState.currentRoute)

        navigationState.selectTopLevelTab(TabStatus.EVENT)
        navigationState.selectTopLevelTab(TabStatus.MAP)

        assertEquals(ScreenRoutes.MapRoutes(), navigationState.currentRoute)
    }

    @Test
    fun `랭킹에서 지도 상세을 열고 뒤로 가면 랭킹 상태로 복귀한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)

        navigationState.showShopOnMap(
            shopId = "shop-id",
            returnTab = TabStatus.RANKING,
        )

        assertTrue(navigationState.canNavigateBack)
        assertEquals(
            ScreenRoutes.MapRoutes("shop-id", TabStatus.RANKING),
            navigationState.currentRoute,
        )

        navigationState.pop()

        assertEquals(TabStatus.RANKING, navigationState.selectedTab)
        assertEquals(ScreenRoutes.RankingTabRoutes, navigationState.currentRoute)
        assertEquals(ScreenRoutes.MapRoutes(), navigationState.backStacks.getValue(TabStatus.MAP).single())
    }

    @Test
    fun `랭킹에서 연 지도 상세을 닫으면 지도에 머물고 복귀 출처를 소비한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)
        navigationState.showShopOnMap(
            shopId = "shop-id",
            returnTab = TabStatus.RANKING,
        )

        navigationState.consumeMapReturnOrigin()

        assertEquals(TabStatus.MAP, navigationState.selectedTab)
        assertEquals(ScreenRoutes.MapRoutes(), navigationState.currentRoute)
        assertFalse(navigationState.canNavigateBack)
    }

    @Test
    fun `같은 공유 링크를 다시 수신하면 별도 지도 진입으로 처리한다`() {
        val state = navigationState()
        state.showShopOnMap("shop-id", source = NavigationSource.SHARED_LINK)
        val first = state.currentRoute as ScreenRoutes.MapRoutes
        state.showShopOnMap("shop-id", source = NavigationSource.SHARED_LINK)
        val second = state.currentRoute as ScreenRoutes.MapRoutes
        assertEquals(first.shopId, second.shopId)
        assertTrue(second.instanceId > first.instanceId)
        state.consumeMapReturnOrigin()
        state.showShopOnMap("shop-id", source = NavigationSource.SHARED_LINK)
        assertTrue((state.currentRoute as ScreenRoutes.MapRoutes).instanceId > second.instanceId)
        assertEquals(1, state.currentBackStack.size)
    }

    @Test
    fun `현재 탭을 다시 선택해도 해당 탭 스택을 유지한다`() {
        val navigationState = navigationState(selectedTab = TabStatus.EVENT)
        navigationState.showEvent("event-id")

        navigationState.selectTopLevelTab(TabStatus.EVENT)

        assertEquals(ScreenRoutes.EventDetailRoutes("event-id"), navigationState.currentRoute)
    }

    @Test
    fun `현재 랭킹 탭을 다시 선택해도 아무 상태를 변경하지 않는다`() {
        val navigationState = navigationState(selectedTab = TabStatus.RANKING)
        val rankingBackStack = navigationState.backStacks.getValue(TabStatus.RANKING).toList()

        navigationState.selectTopLevelTab(TabStatus.RANKING)

        assertEquals(rankingBackStack, navigationState.backStacks.getValue(TabStatus.RANKING).toList())
        assertEquals(TabStatus.RANKING, navigationState.selectedTab)
    }

    @Test
    fun `프로필 탭과 설정의 하위 화면을 전체 화면 경로로 연다`() {
        val navigationState = navigationState(selectedTab = TabStatus.MY)

        navigationState.showSettings()
        navigationState.showSettings()
        assertEquals(ScreenRoutes.SettingsRoutes, navigationState.currentRoute)
        assertEquals(2, navigationState.currentBackStack.size)

        navigationState.pop()

        navigationState.showProfileEdit()
        navigationState.showProfileEdit()
        assertEquals(ScreenRoutes.ProfileEditRoutes, navigationState.currentRoute)
        navigationState.pop()
        assertEquals(ScreenRoutes.MyTabRoutes, navigationState.currentRoute)

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
        navigationState.showImportation()
        assertEquals(ScreenRoutes.ImportationRoutes, navigationState.currentRoute)
        navigationState.showImportationGuide()
        assertEquals(ScreenRoutes.ImportationGuideRoutes, navigationState.currentRoute)
        navigationState.pop()
        assertEquals(ScreenRoutes.ImportationRoutes, navigationState.currentRoute)
        navigationState.pop()
        assertEquals(ScreenRoutes.BookmarkedShopListRoutes, navigationState.currentRoute)
    }

    @Test
    fun `아카이브 진입과 복귀는 기존 지도 엔트리를 유지하고 중복 진입을 막는다`() {
        val state = navigationState()
        val originalMap = state.currentRoute

        state.showArchiveMap()
        state.showArchiveMap()

        assertEquals(listOf(originalMap, ScreenRoutes.ArchiveMapRoutes), state.currentBackStack.toList())
        assertEquals(TabStatus.MAP, state.selectedTab)
        state.pop()
        assertEquals(listOf(originalMap), state.currentBackStack.toList())
    }

    @Test
    fun `아카이브에서 다른 탭을 왕복해도 아카이브 화면을 유지한다`() {
        val state = navigationState()
        state.showArchiveMap()
        state.selectTopLevelTab(TabStatus.MY)
        state.selectTopLevelTab(TabStatus.MAP)
        assertEquals(ScreenRoutes.ArchiveMapRoutes, state.currentRoute)
        state.pop()
        assertEquals(ScreenRoutes.MapRoutes(), state.currentRoute)
    }

    @Test
    fun `아카이브에서 외부 매장 이동은 일반 지도로 연결한다`() {
        val state = navigationState()
        state.showArchiveMap()
        state.showShopOnMap("shop")
        assertEquals(listOf(ScreenRoutes.MapRoutes(shopId = "shop")), state.currentBackStack.toList())
    }

    private fun navigationState(
        selectedTab: TabStatus = TabStatus.MAP,
        rankingBackStack: NavBackStack<NavKey> = NavBackStack(ScreenRoutes.RankingTabRoutes),
    ): NavigationState =
        NavigationState(
            selectedTabState = mutableStateOf(selectedTab),
            backStacks =
                mapOf(
                    TabStatus.MAP to NavBackStack<NavKey>(ScreenRoutes.MapRoutes()),
                    TabStatus.RANKING to rankingBackStack,
                    TabStatus.EVENT to NavBackStack<NavKey>(ScreenRoutes.EventTabRoutes()),
                    TabStatus.FEED to NavBackStack<NavKey>(ScreenRoutes.FeedTabRoutes),
                    TabStatus.MY to NavBackStack<NavKey>(ScreenRoutes.MyTabRoutes),
                ),
        )
}
