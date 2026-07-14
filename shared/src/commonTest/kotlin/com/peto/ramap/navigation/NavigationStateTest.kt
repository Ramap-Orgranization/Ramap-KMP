package com.peto.ramap.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.domain.model.ShopEventType
import com.peto.ramap.ui.main.map.model.TabStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationStateTest {
    @Test
    fun eventDetail_preservesEventTabSelection() {
        val navigationState = NavigationState(NavBackStack<NavKey>(ScreenRoutes.EventTabRoutes))

        navigationState.showEvent(event())

        assertEquals(TabStatus.EVENT, navigationState.selectedTab)
        navigationState.pop()
        assertEquals(ScreenRoutes.EventTabRoutes, navigationState.currentRoute)
    }

    @Test
    fun showingEvent_keepsSelectedEventForDetail() {
        val navigationState = NavigationState(NavBackStack<NavKey>(ScreenRoutes.TabRoutes))
        val event = event()

        navigationState.showEvent(event)

        assertEquals(event, navigationState.selectedEvent)
        assertEquals(ScreenRoutes.EventDetailRoutes(event.id), navigationState.currentRoute)
        assertEquals(TabStatus.MAP, navigationState.selectedTab)
    }

    @Test
    fun showingMapFromEvent_replacesEventFlow() {
        val backStack = NavBackStack<NavKey>(ScreenRoutes.TabRoutes)
        val navigationState = NavigationState(backStack)
        val eventRoute = ScreenRoutes.EventDetailRoutes("event-id")
        navigationState.backStack.add(eventRoute)

        navigationState.showMap()

        assertEquals(ScreenRoutes.TabRoutes, navigationState.currentRoute)
        assertEquals(1, navigationState.backStack.size)
        assertEquals(TabStatus.MAP, navigationState.selectedTab)
    }

    @Test
    fun selectingEventTabAfterShowingMapFromEvent_opensEventTab() {
        val navigationState = NavigationState(NavBackStack<NavKey>(ScreenRoutes.EventTabRoutes))
        navigationState.showEvent(event())
        navigationState.showMap()

        navigationState.selectTopLevelTab(TabStatus.EVENT)

        assertEquals(ScreenRoutes.EventTabRoutes, navigationState.currentRoute)
        assertEquals(TabStatus.EVENT, navigationState.selectedTab)
    }

    private fun event() =
        ShopEvent(
            id = "event-id",
            type = ShopEventType.COLLAB,
            title = "콜라보",
            description = "설명",
            startDate = "2026-07-15",
            endDate = "2026-07-15",
            sourceUrl = "https://instagram.com/event",
            isToday = false,
            isVenue = true,
            venueShopId = "venue-id",
            venueShopName = "행사 매장",
            venueAddress = "서울",
            collaboratorShopId = "collaborator-id",
            collaboratorName = "콜라보 매장",
            collaboratorInstagramUrl = null,
            waitingMethod = null,
            waitingUrl = null,
        )
}
