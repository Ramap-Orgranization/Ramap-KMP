package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.navigation.BackPressController
import com.peto.ramap.navigation.NavigationRouter
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.TabStatus
import com.peto.ramap.navigation.rememberNavigationState
import com.peto.ramap.notification.NotificationDeepLink
import com.peto.ramap.notification.NotificationDeepLinkParser
import com.peto.ramap.notification.NotificationLaunchDispatcher
import com.peto.ramap.ui.account.AccountSettingsRoute
import com.peto.ramap.ui.account.InformationRoute
import com.peto.ramap.ui.bookmark.BookmarkedShopListRoute
import com.peto.ramap.ui.hidden.HiddenShopListRoute
import com.peto.ramap.ui.main.event.EventDetailRoute
import com.peto.ramap.ui.main.event.list.EventsRoute
import com.peto.ramap.ui.main.map.MapRoute
import com.peto.ramap.ui.main.my.MyTabRoute
import com.peto.ramap.ui.main.ranking.RankingRoute
import com.peto.ramap.ui.notification.NotificationSettingsRoute
import com.peto.ramap.ui.report.PlaceReportRoute
import com.peto.ramap.ui.subscribed.SubscribedShopListRoute
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_not_found_message

@Composable
internal fun AppRoute(
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
        navigationState = navigationState,
        mapScreen = { route ->
            MapRoute(
                onEventNavigate = { navigationState.showEvent(it.id) },
                requestedShopId = route.shopId,
            )
        },
        rankingScreen = {
            RankingRoute(
                onShopClick = navigationState::showShopOnMap,
                onFindShopClick = navigationState::showMap,
            )
        },
        myScreen = {
            MyTabRoute(
                onAccountNavigate = navigationState::showAccountSettings,
                onInformationNavigate = navigationState::showInformation,
                onReportNavigate = navigationState::showPlaceReport,
                onHiddenShopsNavigate = navigationState::showHiddenShops,
                onNotificationSettingsNavigate = navigationState::showNotificationSettings,
                onSubscribedShopsNavigate = navigationState::showSubscribedShops,
                onBookmarkedShopsNavigate = navigationState::showBookmarkedShops,
            )
        },
        accountSettingsScreen = {
            AccountSettingsRoute(onBack = navigationState::pop)
        },
        informationScreen = {
            InformationRoute(onBack = navigationState::pop)
        },
        placeReportScreen = {
            PlaceReportRoute(onBack = navigationState::pop)
        },
        eventListScreen = {
            EventsRoute(onEventClick = { navigationState.showEvent(it.id) })
        },
        hiddenScreen = {
            HiddenShopListRoute(
                onBackClick = navigationState::pop,
            )
        },
        notificationSettingsScreen = {
            NotificationSettingsRoute(onBack = navigationState::pop)
        },
        subscribedShopsScreen = {
            SubscribedShopListRoute(onBack = navigationState::pop)
        },
        bookmarkedShopsScreen = {
            BookmarkedShopListRoute(
                onBack = navigationState::pop,
            )
        },
        eventScreen = { route ->
            EventDetailRoute(
                eventId = route.eventId,
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
        },
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
