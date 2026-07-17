package com.peto.ramap.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.peto.ramap.theme.CommonColor

@Composable
fun NavigationRouter(
    navigationState: NavigationState,
    mapScreen: @Composable (ScreenRoutes.TabRoutes) -> Unit,
    eventListScreen: @Composable () -> Unit,
    myScreen: @Composable () -> Unit,
    hiddenScreen: @Composable () -> Unit,
    notificationSettingsScreen: @Composable () -> Unit,
    eventScreen: @Composable (ScreenRoutes.EventDetailRoutes) -> Unit,
) {
    val routeEntryProvider: (NavKey) -> NavEntry<NavKey> =
        entryProvider {
            entry<ScreenRoutes.TabRoutes> { route ->
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.MAP,
                    onTabSelected = navigationState::selectTopLevelTab,
                    content = { mapScreen(route) },
                )
            }
            entry<ScreenRoutes.EventTabRoutes> {
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.EVENT,
                    onTabSelected = navigationState::selectTopLevelTab,
                    content = eventListScreen,
                )
            }
            entry<ScreenRoutes.MyTabRoutes> {
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.MY,
                    onTabSelected = navigationState::selectTopLevelTab,
                    content = myScreen,
                )
            }
            entry<ScreenRoutes.HiddenShopListRoutes> {
                FullScreen(hiddenScreen)
            }
            entry<ScreenRoutes.NotificationSettingsRoutes> {
                FullScreen(notificationSettingsScreen)
            }
            entry<ScreenRoutes.EventDetailRoutes> { route ->
                FullScreen { (eventScreen(route)) }
            }
        }
    val decoratedEntries =
        rememberDecoratedEntries(
            navigationState = navigationState,
            entryProvider = routeEntryProvider,
        )

    NavDisplay(
        entries = decoratedEntries.getValue(navigationState.selectedTab),
        modifier = Modifier.fillMaxSize().background(CommonColor.White),
        onBack = navigationState::pop,
    )
}

@Composable
private fun rememberDecoratedEntries(
    navigationState: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): Map<TabStatus, List<NavEntry<NavKey>>> =
    navigationState.backStacks.mapValues { (tab, backStack) ->
        key(tab) {
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                entryProvider = entryProvider,
            )
        }
    }

@Composable
private fun BottonNavigationTabScreen(
    selectedTab: TabStatus,
    onTabSelected: (TabStatus) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            content()
        }

        NavigationBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun FullScreen(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White),
    ) {
        content()
    }
}
