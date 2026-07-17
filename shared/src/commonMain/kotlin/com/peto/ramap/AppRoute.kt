package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.navigation.BackPressController
import com.peto.ramap.navigation.NavigationRouter
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.rememberNavigationState
import com.peto.ramap.notification.NotificationDeepLink
import com.peto.ramap.notification.NotificationDeepLinkParser
import com.peto.ramap.notification.NotificationLaunchDispatcher
import com.peto.ramap.screen.RootEventDetailScreen
import com.peto.ramap.screen.RootEventListScreen
import com.peto.ramap.screen.RootHiddenScreen
import com.peto.ramap.screen.RootMapScreen
import com.peto.ramap.screen.RootMyScreen
import com.peto.ramap.screen.RootNotificationSettingsScreen
import org.koin.compose.koinInject

@Composable
fun AppRoute(
    toastManager: ToastManager,
    onExitRequested: (() -> Unit)?,
    notificationDeepLinkParser: NotificationDeepLinkParser = koinInject(),
    notificationLaunchDispatcher: NotificationLaunchDispatcher = koinInject(),
) {
    val navigationState = rememberNavigationState()

    NotificationDeepLink(
        navigationState = navigationState,
        notificationDeepLinkParser = notificationDeepLinkParser,
        notificationLaunchDispatcher = notificationLaunchDispatcher,
    )

    BackPressController(
        navigationState = navigationState,
        onExitRequested = onExitRequested,
        toastManager = toastManager,
    )

    NavigationRouter(
        currentRoute = navigationState.currentRoute,
        selectedTab = navigationState.selectedTab,
        onTabSelected = navigationState::selectTopLevelTab,
        mapScreen = { RootMapScreen(navigationState) },
        myScreen = { RootMyScreen(navigationState) },
        eventListScreen = { RootEventListScreen(navigationState) },
        hiddenScreen = { RootHiddenScreen(navigationState) },
        notificationSettingsScreen = { RootNotificationSettingsScreen(navigationState) },
        eventScreen = { RootEventDetailScreen(navigationState, toastManager) },
    )
}

@Composable
private fun NotificationDeepLink(
    navigationState: NavigationState,
    notificationDeepLinkParser: NotificationDeepLinkParser,
    notificationLaunchDispatcher: NotificationLaunchDispatcher,
) {
    LaunchedEffect(navigationState) {
        notificationLaunchDispatcher.pendingDeepLink.collect { value ->
            val deepLink = notificationDeepLinkParser.parse(value)
            val eventDeepLink = deepLink as? NotificationDeepLink.Event

            eventDeepLink?.let {
                navigationState.showEvent(it.eventId)
                notificationLaunchDispatcher.consume(requireNotNull(value))
            }
        }
    }
}
