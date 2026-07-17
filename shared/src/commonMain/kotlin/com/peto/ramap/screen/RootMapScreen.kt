package com.peto.ramap.screen

import androidx.compose.runtime.Composable
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.ScreenRoutes
import com.peto.ramap.navigation.TabStatus
import com.peto.ramap.ui.hidden.HiddenShopListRoute
import com.peto.ramap.ui.main.event.EventDetailRoute
import com.peto.ramap.ui.main.event.list.EventListRoute
import com.peto.ramap.ui.main.map.MapRoute
import com.peto.ramap.ui.main.my.MyTabRoute
import com.peto.ramap.ui.settings.notification.NotificationSettingsRoute
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_not_found_message

@Composable
internal fun RootMapScreen(navigationState: NavigationState) {
    val isMapBackEnabled = navigationState.currentRoute !is ScreenRoutes.EventDetailRoutes

    MapRoute(
        isBackEnabled = isMapBackEnabled,
        onEventNavigate = navigationState::showEvent,
        requestedShopId = navigationState.requestedMapShopId,
        onRequestedShopConsumed = navigationState::consumeMapShopRequest,
    )
}

@Composable
internal fun RootMyScreen(navigationState: NavigationState) {
    MyTabRoute(
        onHiddenShopsNavigate = navigationState::showHiddenShops,
        onNotificationSettingsNavigate = navigationState::showNotificationSettings,
    )
}

@Composable
internal fun RootEventListScreen(navigationState: NavigationState) {
    EventListRoute(onEventClick = navigationState::showEvent)
}

@Composable
internal fun RootHiddenScreen(navigationState: NavigationState) {
    HiddenShopListRoute(
        onBackClick = navigationState::pop,
        onShopClick = navigationState::showShopOnMap,
    )
}

@Composable
internal fun RootNotificationSettingsScreen(navigationState: NavigationState) {
    NotificationSettingsRoute(onBack = navigationState::pop)
}

@Composable
internal fun RootEventDetailScreen(
    navigationState: NavigationState,
    toastManager: ToastManager,
) {
    val route = navigationState.currentRoute as ScreenRoutes.EventDetailRoutes
    val selectedEvent = navigationState.selectedEvent
    val isSelectedEventMatchingRoute = selectedEvent?.id == route.eventId

    EventDetailRoute(
        eventId = route.eventId,
        initialEvent = selectedEvent.takeIf { isSelectedEventMatchingRoute },
        onBack = navigationState::pop,
        onUnavailable = {
            navigationState.selectTopLevelTab(TabStatus.EVENT)
            toastManager.tryShow(
                ToastData(
                    Res.string.event_not_found_message,
                    ToastType.DEFAULT,
                ),
            )
        },
        onShopClick = navigationState::showShopOnMap,
    )
}
