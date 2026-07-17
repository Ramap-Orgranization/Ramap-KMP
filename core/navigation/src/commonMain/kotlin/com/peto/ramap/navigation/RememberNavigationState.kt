package com.peto.ramap.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Composable
fun rememberNavigationState(): NavigationState {
    val navigationConfiguration = rememberNavigationConfiguration()
    val backStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.TabRoutes)

    return remember(backStack) {
        backStack.removeAll { it is ScreenRoutes.EventDetailRoutes }
        NavigationState(backStack)
    }
}

@Composable
private fun rememberNavigationConfiguration(): SavedStateConfiguration =
    remember {
        SavedStateConfiguration {
            serializersModule = navKeySerializersModule()
        }
    }

private fun navKeySerializersModule(): SerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(ScreenRoutes.TabRoutes::class)
            subclass(ScreenRoutes.EventTabRoutes::class)
            subclass(ScreenRoutes.MyTabRoutes::class)
            subclass(ScreenRoutes.HiddenShopListRoutes::class)
            subclass(ScreenRoutes.NotificationSettingsRoutes::class)
            subclass(ScreenRoutes.EventDetailRoutes::class)
        }
    }
