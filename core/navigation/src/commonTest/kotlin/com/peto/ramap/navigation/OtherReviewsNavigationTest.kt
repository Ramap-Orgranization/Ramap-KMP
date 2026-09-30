package com.peto.ramap.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OtherReviewsNavigationTest {
    @Test
    fun `other reviews route restores the selected author`() {
        val route = ScreenRoutes.OtherReviewsRoutes("author-id")
        val json = Json { serializersModule = navKeySerializersModule() }
        val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), route)

        assertEquals(route, json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded))
    }

    @Test
    fun `opening an author returns to the originating tab`() {
        val state =
            NavigationState(
                selectedTabState = mutableStateOf(TabStatus.MAP),
                backStacks =
                    mapOf(
                        TabStatus.MAP to NavBackStack<NavKey>(ScreenRoutes.MapRoutes()),
                        TabStatus.RANKING to NavBackStack<NavKey>(ScreenRoutes.RankingTabRoutes),
                        TabStatus.EVENT to NavBackStack<NavKey>(ScreenRoutes.EventTabRoutes()),
                        TabStatus.MY to NavBackStack<NavKey>(ScreenRoutes.MyTabRoutes),
                    ),
            )
        state.showOtherReviews("author-id")
        assertEquals(ScreenRoutes.OtherReviewsRoutes("author-id"), state.currentRoute)

        state.pop()
        assertEquals(TabStatus.MAP, state.selectedTab)
        assertEquals(ScreenRoutes.MapRoutes(), state.currentRoute)
    }
}
