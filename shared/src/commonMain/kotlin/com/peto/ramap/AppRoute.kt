package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.peto.ramap.deeplink.DeepLinkEntryPoint
import com.peto.ramap.deeplink.DeepLinkEvent
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.log.AppAnalytics
import com.peto.ramap.log.analyticsScreenName
import com.peto.ramap.navigation.BackPressController
import com.peto.ramap.navigation.NavigationRouter
import com.peto.ramap.navigation.NavigationSource
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
import com.peto.ramap.ui.bookmark.importation.ImportationRoute
import com.peto.ramap.ui.bookmark.list.BookmarkedShopListRoute
import com.peto.ramap.ui.hidden.HiddenShopListRoute
import com.peto.ramap.ui.main.event.EventDetailRoute
import com.peto.ramap.ui.main.event.list.EventsRoute
import com.peto.ramap.ui.main.map.MapRoute
import com.peto.ramap.ui.main.map.MapViewModel
import com.peto.ramap.ui.main.map.ShopDetailHost
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMapTabExited
import com.peto.ramap.ui.main.my.MyTabRoute
import com.peto.ramap.ui.main.ranking.RankingRoute
import com.peto.ramap.ui.notification.NotificationSettingsRoute
import com.peto.ramap.ui.report.PlaceReportRoute
import com.peto.ramap.ui.subscribed.SubscribedShopListRoute
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
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
    val mapViewModel = koinViewModel<MapViewModel>()
    val navigationState =
        rememberNavigationState(
            onMapTabExited = { mapViewModel.dispatch(OnMapTabExited) },
        )

    HandleDeepLinkEvents(deepLinkEntryPoint, notificationLaunchDispatcher, shopDeepLinkDispatcher, appAnalytics)

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
                isBackEnabled = route.returnTab == null,
                onDetailDismissed = navigationState::consumeMapReturnOrigin,
                onEventNavigate = { event ->
                    navigationState.showEvent(event.id)
                },
                requestedShopId = route.shopId,
                showShopDetail = route.showShopDetail,
                viewModel = mapViewModel,
            )
        },
        rankingScreen = {
            RankingRoute(
                onFindShopClick = navigationState::showMap,
                onShowShopOnMap = { navigationState.showMap() },
                onEventNavigate = { event -> navigationState.showEvent(event.id) },
                shopDetailContent = { shopId, onDismiss, onShowOnMap, onEventNavigate ->
                    ShopDetailHost(
                        shopId = shopId,
                        viewModel = mapViewModel,
                        onDismiss = onDismiss,
                        onShowOnMap = onShowOnMap,
                        onEventNavigate = { event -> onEventNavigate(event) },
                    )
                },
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
                onShopOpen = { shopId ->
                    navigationState.showShopOnMap(
                        shopId,
                        source = NavigationSource.HIDDEN_SHOPS,
                        returnTab = TabStatus.MY,
                    )
                },
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
                onShopOpen = { shopId ->
                    navigationState.showShopOnMap(
                        shopId,
                        source = NavigationSource.SUBSCRIBED_SHOPS,
                        returnTab = TabStatus.MY,
                    )
                },
                onEventOpen = navigationState::showEvent,
            )
        },
        bookmarkedShopsScreen = {
            BookmarkedShopListRoute(
                onBack = navigationState::pop,
                onImportationNavigate = navigationState::showImportation,
                onShopOpen = { shopId ->
                    navigationState.showShopOnMap(
                        shopId,
                        source = NavigationSource.BOOKMARKED_SHOPS,
                        returnTab = TabStatus.MY,
                    )
                },
            )
        },
        importationScreen = {
            ImportationRoute(
                onBack = navigationState::pop,
                onImportCompleted = navigationState::pop,
            )
        },
        eventScreen = { route ->
            EventDetailRoute(
                eventId = route.eventId,
                onBack = navigationState::pop,
                onUnavailable = {
                    navigationState.showEventRoot()

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
                        source = NavigationSource.EVENT_DETAIL,
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
    appAnalytics: AppAnalytics,
) {
    LaunchedEffect(entryPoint) {
        entryPoint.events.filterNotNull().collect { event ->
            when (event) {
                is DeepLinkEvent.Url -> {
                    appAnalytics.logDeepLinkReceived()
                    if (!shopDispatcher.dispatch(event.value)) appAnalytics.logDeepLinkParseFailed()
                }
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
                appAnalytics.logDeepLinkParseSucceeded(deepLink.shopId)
                appAnalytics.logSharedShopLinkOpened(deepLink.shopId)
                navigationState.showShopOnMap(
                    shopId = deepLink.shopId,
                    source = NavigationSource.SHARED_LINK,
                )
                appAnalytics.logDeepLinkNavigationSucceeded(deepLink.shopId)
            } else {
                appAnalytics.logDeepLinkNavigationFailed()
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
        personalizationStore.state.collect { state ->
            if (state is PersonalizationBootstrapState.Success) {
                appAnalytics.updatePersonalizationProperties(state.value)
            }
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
