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
    mapScreen: @Composable (ScreenRoutes.MapRoutes) -> Unit,
    rankingScreen: @Composable () -> Unit,
    eventListScreen: @Composable () -> Unit,
    eventCalendarScreen: @Composable () -> Unit,
    operatingNoticeScreen: @Composable () -> Unit,
    myScreen: @Composable () -> Unit,
    accountSettingsScreen: @Composable () -> Unit,
    informationScreen: @Composable () -> Unit,
    placeReportScreen: @Composable () -> Unit,
    hiddenScreen: @Composable () -> Unit,
    notificationSettingsScreen: @Composable () -> Unit,
    subscribedShopsScreen: @Composable () -> Unit,
    bookmarkedShopsScreen: @Composable () -> Unit,
    importationScreen: @Composable () -> Unit,
    importationGuideScreen: @Composable () -> Unit,
    eventScreen: @Composable (ScreenRoutes.EventDetailRoutes) -> Unit,
) {
    val onTabSelected: (TabStatus) -> Unit = navigationState::selectTopLevelTab
    val routeEntryProvider: (NavKey) -> NavEntry<NavKey> =
        entryProvider {
            entry<ScreenRoutes.MapRoutes> { route ->
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.MAP,
                    onTabSelected = onTabSelected,
                    content = { mapScreen(route) },
                )
            }
            entry<ScreenRoutes.EventTabRoutes> {
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.EVENT,
                    onTabSelected = onTabSelected,
                    content = eventListScreen,
                )
            }
            entry<ScreenRoutes.EventCalendarRoutes> {
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.EVENT,
                    onTabSelected = onTabSelected,
                    content = eventCalendarScreen,
                )
            }
            entry<ScreenRoutes.OperatingNoticeRoutes> {
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.EVENT,
                    onTabSelected = onTabSelected,
                    content = operatingNoticeScreen,
                )
            }
            entry<ScreenRoutes.RankingTabRoutes> {
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.RANKING,
                    onTabSelected = onTabSelected,
                    content = rankingScreen,
                )
            }
            entry<ScreenRoutes.MyTabRoutes> {
                BottonNavigationTabScreen(
                    selectedTab = TabStatus.MY,
                    onTabSelected = onTabSelected,
                    content = myScreen,
                )
            }
            entry<ScreenRoutes.AccountSettingsRoutes> { FullScreen(accountSettingsScreen) }
            entry<ScreenRoutes.InformationRoutes> { FullScreen(informationScreen) }
            entry<ScreenRoutes.PlaceReportRoutes> { FullScreen(placeReportScreen) }
            entry<ScreenRoutes.HiddenShopListRoutes> {
                FullScreen(hiddenScreen)
            }
            entry<ScreenRoutes.NotificationSettingsRoutes> {
                FullScreen(notificationSettingsScreen)
            }
            entry<ScreenRoutes.SubscribedShopListRoutes> {
                FullScreen(subscribedShopsScreen)
            }
            entry<ScreenRoutes.BookmarkedShopListRoutes> {
                FullScreen(bookmarkedShopsScreen)
            }
            entry<ScreenRoutes.ImportationRoutes> {
                FullScreen(importationScreen)
            }
            entry<ScreenRoutes.ImportationGuideRoutes> {
                FullScreen(importationGuideScreen)
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
