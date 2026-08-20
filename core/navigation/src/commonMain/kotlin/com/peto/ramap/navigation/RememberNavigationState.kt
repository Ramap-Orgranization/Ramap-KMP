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
fun rememberNavigationState(onMapTabExited: () -> Unit = {}): NavigationState {
    val navigationConfiguration = rememberNavigationConfiguration()
    val selectedTabState =
        rememberSerializable(
            configuration = navigationConfiguration,
            stateSerializer = TabStatus.serializer(),
        ) {
            mutableStateOf(TabStatus.MAP)
        }
    val mapBackStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.MapRoutes())
    val eventBackStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.EventTabRoutes())
    val rankingBackStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.RankingTabRoutes)
    val myBackStack =
        rememberNavBackStack(configuration = navigationConfiguration, ScreenRoutes.MyTabRoutes)
    val backStacks =
        remember(mapBackStack, rankingBackStack, eventBackStack, myBackStack) {
            mapOf(
                TabStatus.MAP to mapBackStack,
                TabStatus.RANKING to rankingBackStack,
                TabStatus.EVENT to eventBackStack,
                TabStatus.MY to myBackStack,
            )
        }

    return remember(selectedTabState, backStacks, onMapTabExited) {
        NavigationState(
            selectedTabState = selectedTabState,
            backStacks = backStacks,
            onMapTabExited = onMapTabExited,
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
            subclass(ScreenRoutes.MapRoutes::class)
            subclass(ScreenRoutes.EventTabRoutes::class)
            subclass(ScreenRoutes.EventCalendarRoutes::class)
            subclass(ScreenRoutes.RankingTabRoutes::class)
            subclass(ScreenRoutes.MyTabRoutes::class)
            subclass(ScreenRoutes.AccountSettingsRoutes::class)
            subclass(ScreenRoutes.InformationRoutes::class)
            subclass(ScreenRoutes.PlaceReportRoutes::class)
            subclass(ScreenRoutes.HiddenShopListRoutes::class)
            subclass(ScreenRoutes.NotificationSettingsRoutes::class)
            subclass(ScreenRoutes.SubscribedShopListRoutes::class)
            subclass(ScreenRoutes.BookmarkedShopListRoutes::class)
            subclass(ScreenRoutes.ImportationRoutes::class)
            subclass(ScreenRoutes.ImportationGuideRoutes::class)
            subclass(ScreenRoutes.EventDetailRoutes::class)
        }
    }
