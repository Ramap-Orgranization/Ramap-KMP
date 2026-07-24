package com.peto.ramap.analytics

import com.peto.ramap.navigation.ScreenRoutes
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenNamesTest {
    @Test
    fun allRoutesHaveDistinctScreenNames() {
        val routes =
            listOf(
                ScreenRoutes.TabRoutes(),
                ScreenRoutes.EventTabRoutes,
                ScreenRoutes.RankingTabRoutes,
                ScreenRoutes.MyTabRoutes,
                ScreenRoutes.AccountSettingsRoutes,
                ScreenRoutes.InformationRoutes,
                ScreenRoutes.PlaceReportRoutes,
                ScreenRoutes.HiddenShopListRoutes,
                ScreenRoutes.NotificationSettingsRoutes,
                ScreenRoutes.SubscribedShopListRoutes,
                ScreenRoutes.BookmarkedShopListRoutes,
                ScreenRoutes.EventDetailRoutes("test-id"),
            )
        val names = routes.map { it.analyticsScreenName }
        assertEquals(names.size, names.toSet().size, "Screen names must be unique")
    }

    @Test
    fun mapRouteReturnsMap() {
        assertEquals("map", ScreenRoutes.TabRoutes().analyticsScreenName)
    }

    @Test
    fun eventDetailRouteReturnsEventDetail() {
        assertEquals("event_detail", ScreenRoutes.EventDetailRoutes("id").analyticsScreenName)
    }
}
