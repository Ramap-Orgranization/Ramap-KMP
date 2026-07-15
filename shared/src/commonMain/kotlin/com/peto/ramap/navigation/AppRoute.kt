package com.peto.ramap.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.notification.NotificationDeepLink
import com.peto.ramap.notification.NotificationDeepLinkParser
import com.peto.ramap.notification.NotificationLaunchDispatcher
import com.peto.ramap.ui.hidden.HiddenShopListRoute
import com.peto.ramap.ui.main.event.EventDetailRoute
import com.peto.ramap.ui.main.event.list.EventListRoute
import com.peto.ramap.ui.main.map.MapRoute
import com.peto.ramap.ui.main.map.MapViewModel
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.model.TabStatus
import com.peto.ramap.ui.main.my.MyTabRoute
import com.peto.ramap.ui.settings.notification.NotificationSettingsRoute
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.app_exit_back_message
import ramap.shared.generated.resources.event_not_found_message
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
fun AppRoute(
    toastManager: ToastManager,
    onExitRequested: (() -> Unit)?,
    notificationDeepLinkParser: NotificationDeepLinkParser = koinInject(),
    notificationLaunchDispatcher: NotificationLaunchDispatcher = koinInject(),
) {
    val navigationState = rememberNavigationState()
    val mapViewModel: MapViewModel = koinViewModel()
    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )
    var previousBackPress by remember { mutableStateOf(TimeSource.Monotonic.markNow() - EXIT_INTERVAL) }

    LaunchedEffect(navigationState) {
        notificationLaunchDispatcher.pendingDeepLink.collect { value ->
            val deepLink = notificationDeepLinkParser.parse(value)
            if (deepLink is NotificationDeepLink.Event) {
                navigationState.showEvent(deepLink.eventId)
                notificationLaunchDispatcher.consume(requireNotNull(value))
            }
        }
    }

    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = navigationState.backStack.size > 1 || onExitRequested != null,
        onBackCompleted = {
            if (navigationState.backStack.size > 1) {
                navigationState.pop()
                return@NavigationBackHandler
            }

            val now = TimeSource.Monotonic.markNow()
            if (now - previousBackPress <= EXIT_INTERVAL) {
                onExitRequested?.invoke()
            } else {
                previousBackPress = now
                toastManager.tryShow(
                    ToastData(
                        message = Res.string.app_exit_back_message,
                        type = ToastType.DEFAULT,
                    ),
                )
            }
        },
    )

    NavigationRouter(
        currentRoute = navigationState.currentRoute,
        selectedTab = navigationState.selectedTab,
        onTabSelected = navigationState::selectTopLevelTab,
        mapContent = {
            MapRoute(
                isBackEnabled = navigationState.currentRoute !is ScreenRoutes.EventDetailRoutes,
                onEventNavigate = navigationState::showEvent,
                viewModel = mapViewModel,
            )
        },
        myContent = {
            MyTabRoute(
                onHiddenShopsNavigate = navigationState::showHiddenShops,
                onNotificationSettingsNavigate = navigationState::showNotificationSettings,
            )
        },
        eventListContent = {
            EventListRoute(onEventClick = navigationState::showEvent)
        },
        hiddenContent = {
            HiddenShopListRoute(
                onBackClick = navigationState::pop,
                onShopClick = { shop ->
                    mapViewModel.dispatch(OnShopSelected(shop))
                    navigationState.showMap()
                },
            )
        },
        notificationSettingsContent = {
            NotificationSettingsRoute(onBack = navigationState::pop)
        },
        eventContent = {
            val route = navigationState.currentRoute as ScreenRoutes.EventDetailRoutes
            EventDetailRoute(
                eventId = route.eventId,
                initialEvent = navigationState.selectedEvent?.takeIf { it.id == route.eventId },
                onBack = navigationState::pop,
                onUnavailable = {
                    navigationState.selectTopLevelTab(TabStatus.EVENT)
                    toastManager.tryShow(ToastData(Res.string.event_not_found_message, ToastType.DEFAULT))
                },
                onShopClick = { shopId ->
                    mapViewModel.dispatch(OnShopIdSelected(shopId))
                    navigationState.showMap()
                },
            )
        },
    )
}

private val EXIT_INTERVAL = 2.seconds
