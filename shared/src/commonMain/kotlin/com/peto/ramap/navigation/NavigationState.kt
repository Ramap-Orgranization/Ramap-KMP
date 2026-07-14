package com.peto.ramap.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.ui.main.map.model.TabStatus
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class NavigationState(
    val backStack: NavBackStack<NavKey>,
) {
    var selectedEvent by mutableStateOf<ShopEvent?>(null)
        private set

    val currentRoute: ScreenRoutes
        get() = backStack.last() as ScreenRoutes

    val selectedTab: TabStatus
        get() =
            when (currentRoute) {
                ScreenRoutes.TabRoutes -> TabStatus.MAP
                ScreenRoutes.MyTabRoutes,
                ScreenRoutes.HiddenShopListRoutes,
                is ScreenRoutes.EventDetailRoutes,
                -> TabStatus.MY
            }

    fun showHiddenShops() {
        if (currentRoute != ScreenRoutes.HiddenShopListRoutes) {
            backStack.add(ScreenRoutes.HiddenShopListRoutes)
        }
    }

    fun showEvent(event: ShopEvent) {
        selectedEvent = event
        backStack.add(ScreenRoutes.EventDetailRoutes(event.id))
    }

    fun pop() {
        backStack.removeLastOrNull()
    }

    fun selectTopLevelTab(tab: TabStatus) {
        val rootRoute = tab.toRootRoute()
        val currentRoot = backStack.firstOrNull()
        if (currentRoot == rootRoute && backStack.isNotEmpty()) return

        backStack.clear()
        backStack.add(rootRoute)
        selectedEvent = null
    }

    fun showMap() {
        if (currentRoute is ScreenRoutes.EventDetailRoutes) {
            backStack.add(ScreenRoutes.TabRoutes)
            return
        }

        selectTopLevelTab(TabStatus.MAP)
    }
}

@Composable
fun rememberNavigationState(): NavigationState {
    val navigationConfiguration =
        remember {
            SavedStateConfiguration {
                serializersModule =
                    SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(ScreenRoutes.TabRoutes::class)
                            subclass(ScreenRoutes.MyTabRoutes::class)
                            subclass(ScreenRoutes.HiddenShopListRoutes::class)
                            subclass(ScreenRoutes.EventDetailRoutes::class)
                        }
                    }
            }
        }
    val backStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.TabRoutes)
    return remember(backStack) {
        backStack.removeAll { it is ScreenRoutes.EventDetailRoutes }
        NavigationState(backStack)
    }
}

private fun TabStatus.toRootRoute(): ScreenRoutes =
    when (this) {
        TabStatus.MAP -> ScreenRoutes.TabRoutes
        TabStatus.MY -> ScreenRoutes.MyTabRoutes
    }
