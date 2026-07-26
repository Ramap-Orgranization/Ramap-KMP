package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.peto.ramap.deeplink.DeepLinkEntryPoint
import com.peto.ramap.deeplink.DeepLinkEvent
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.log.AppAnalytics
import com.peto.ramap.log.analyticsScreenName
import com.peto.ramap.navigation.BackPressController
import com.peto.ramap.navigation.NavigationRouter
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.TabStatus
import com.peto.ramap.navigation.deeplink.ShopDeepLink
import com.peto.ramap.navigation.deeplink.ShopDeepLinkDispatcher
import com.peto.ramap.navigation.deeplink.ShopDeepLinkParser
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_not_found_message

@Composable
internal fun AppRoute(
    toastManager: ToastManager,
    onExitRequested: (() -> Unit)?,
    notificationDeepLinkParser: NotificationDeepLinkParser = koinInject(),
    notificationLaunchDispatcher: NotificationLaunchDispatcher = koinInject(),
    shopDeepLinkParser: ShopDeepLinkParser = koinInject(),
    shopDeepLinkDispatcher: ShopDeepLinkDispatcher = koinInject(),
    deepLinkEntryPoint: DeepLinkEntryPoint = koinInject(),
    appAnalytics: AppAnalytics = koinInject(),
    loginRepository: LoginRepository = koinInject(),
    personalizationStore: ShopPersonalizationStore = koinInject(),
) {
    val navigationState = rememberNavigationState()

    HandleDeepLinkEvents(deepLinkEntryPoint, notificationLaunchDispatcher, shopDeepLinkDispatcher)

    TrackScreenViews(navigationState, appAnalytics)

    TrackUserProperties(
        appAnalytics = appAnalytics,
        loginRepository = loginRepository,
        personalizationStore = personalizationStore,
    )

    HandleNotificationDeepLink(
        navigationState = navigationState,
        notificationDeepLinkParser = notificationDeepLinkParser,
        notificationLaunchDispatcher = notificationLaunchDispatcher,
        appAnalytics = appAnalytics,
    )

    HandleShopDeepLink(
        navigationState = navigationState,
        parser = shopDeepLinkParser,
        dispatcher = shopDeepLinkDispatcher,
        appAnalytics = appAnalytics,
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
                onEventNavigate = { event ->
                    navigationState.showEvent(event.id)
                },
                requestedShopId = route.shopId,
            )
        },
        rankingScreen = {
            RankingRoute(
                onShopClick = { shopId ->
                    navigationState.showShopOnMap(
                        shopId = shopId,
                        source = "ranking",
                    )
                },
                onFindShopClick = navigationState::showMap,
            )
        },
        myScreen = {
            MyTabRoute(
                onAccountNavigate = navigationState::showAccountSettings,
                onInformationNavigate = navigationState::showInformation,
                onReportNavigate = navigationState::showPlaceReport,
                onHiddenShopsNavigate = navigationState::showHiddenShops,
                onNotificationSettingsNavigate =
                    navigationState::showNotificationSettings,
                onSubscribedShopsNavigate =
                    navigationState::showSubscribedShops,
                onBookmarkedShopsNavigate =
                    navigationState::showBookmarkedShops,
            )
        },
        accountSettingsScreen = {
            AccountSettingsRoute(
                onBack = navigationState::pop,
            )
        },
        informationScreen = {
            InformationRoute(
                onBack = navigationState::pop,
            )
        },
        placeReportScreen = {
            PlaceReportRoute(
                onBack = navigationState::pop,
            )
        },
        eventListScreen = {
            EventsRoute(
                onEventClick = { event ->
                    navigationState.showEvent(event.id)
                },
            )
        },
        hiddenScreen = {
            HiddenShopListRoute(
                onBackClick = navigationState::pop,
            )
        },
        notificationSettingsScreen = {
            NotificationSettingsRoute(
                onBack = navigationState::pop,
            )
        },
        subscribedShopsScreen = {
            SubscribedShopListRoute(
                onBack = navigationState::pop,
            )
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
                    navigationState.selectTopLevelTab(
                        TabStatus.EVENT,
                    )

                    toastManager.tryShow(
                        ToastData(
                            message = Res.string.event_not_found_message,
                            type = ToastType.DEFAULT,
                        ),
                    )
                },
                onShopClick = { shopId ->
                    navigationState.showShopOnMap(
                        shopId = shopId,
                        source = "event_detail",
                    )
                },
            )
        },
    )
}

@Composable
private fun HandleDeepLinkEvents(
    entryPoint: DeepLinkEntryPoint,
    notificationDispatcher: NotificationLaunchDispatcher,
    shopDispatcher: ShopDeepLinkDispatcher,
) {
    LaunchedEffect(entryPoint) {
        entryPoint.events.filterNotNull().collect { event ->
            when (event) {
                is DeepLinkEvent.Url -> shopDispatcher.dispatch(event.value)
                is DeepLinkEvent.Notification -> notificationDispatcher.dispatch(event.value)
            }
            entryPoint.consume(event)
        }
    }
}

@Composable
private fun HandleShopDeepLink(
    navigationState: NavigationState,
    parser: ShopDeepLinkParser,
    dispatcher: ShopDeepLinkDispatcher,
    appAnalytics: AppAnalytics,
) {
    LaunchedEffect(navigationState, parser, dispatcher) {
        dispatcher.pendingDeepLink.collect { value ->
            if (value == null) return@collect
            val deepLink = parser.parse(value)
            if (deepLink is ShopDeepLink.Shop) {
                appAnalytics.logSharedShopLinkOpened(deepLink.shopId)
                navigationState.showShopOnMap(
                    shopId = deepLink.shopId,
                    source = "shared_link",
                )
            }
            dispatcher.consume(value)
        }
    }
}

@Composable
private fun TrackScreenViews(
    navigationState: NavigationState,
    appAnalytics: AppAnalytics,
) {
    LaunchedEffect(navigationState) {
        snapshotFlow {
            navigationState.currentRoute
        }.distinctUntilChanged()
            .collect { route ->
                appAnalytics.logScreenView(
                    route.analyticsScreenName,
                )
            }
    }
}

@Composable
private fun TrackUserProperties(
    appAnalytics: AppAnalytics,
    loginRepository: LoginRepository,
    personalizationStore: ShopPersonalizationStore,
) {
    LaunchedEffect(loginRepository) {
        loginRepository.sessionState.collect { state ->
            appAnalytics.updateLoginStatus(state)
        }
    }

    LaunchedEffect(personalizationStore) {
        personalizationStore.state.collect { personalization ->
            appAnalytics.updatePersonalizationProperties(
                personalization,
            )
        }
    }
}

@Composable
private fun HandleNotificationDeepLink(
    navigationState: NavigationState,
    notificationDeepLinkParser: NotificationDeepLinkParser,
    notificationLaunchDispatcher: NotificationLaunchDispatcher,
    appAnalytics: AppAnalytics,
) {
    LaunchedEffect(
        navigationState,
        notificationDeepLinkParser,
        notificationLaunchDispatcher,
    ) {
        notificationLaunchDispatcher.pendingDeepLink.collect { value ->
            if (value == null) return@collect

            val deepLink = notificationDeepLinkParser.parse(value)
            if (deepLink is NotificationDeepLink.Event) {
                appAnalytics.logNotificationOpened(
                    eventId = deepLink.eventId,
                )

                navigationState.showEvent(
                    deepLink.eventId,
                )
            } else {
                // TODO: 다른 타입의 딥링크 처리 또는 미지원 로깅
            }

            notificationLaunchDispatcher.consume(value)
        }
    }
}
