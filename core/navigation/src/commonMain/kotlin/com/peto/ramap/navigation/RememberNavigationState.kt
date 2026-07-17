package com.peto.ramap.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Composable
fun rememberNavigationState(): NavigationState {
    val navigationConfiguration = rememberNavigationConfiguration()
    val selectedTabState =
        rememberSerializable(
            configuration = navigationConfiguration,
            stateSerializer = TabStatus.serializer(),
        ) {
            mutableStateOf(TabStatus.MAP)
        }
    val mapBackStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.TabRoutes())
    val eventBackStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.EventTabRoutes)
    val myBackStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.MyTabRoutes)
    val backStacks =
        remember(mapBackStack, eventBackStack, myBackStack) {
            mapOf(
                TabStatus.MAP to mapBackStack,
                TabStatus.EVENT to eventBackStack,
                TabStatus.MY to myBackStack,
            )
        }

    return remember(selectedTabState, backStacks) {
        NavigationState(
            selectedTabState = selectedTabState,
            backStacks = backStacks,
        )
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
            subclass(ScreenRoutes.AccountSettingsRoutes::class)
            subclass(ScreenRoutes.InformationRoutes::class)
            subclass(ScreenRoutes.PlaceReportRoutes::class)
            subclass(ScreenRoutes.HiddenShopListRoutes::class)
            subclass(ScreenRoutes.NotificationSettingsRoutes::class)
            subclass(ScreenRoutes.SubscribedShopListRoutes::class)
            subclass(ScreenRoutes.BookmarkedShopListRoutes::class)
            subclass(ScreenRoutes.EventDetailRoutes::class)
        }
    }
