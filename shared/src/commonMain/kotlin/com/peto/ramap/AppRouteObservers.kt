package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.peto.ramap.deeplink.DeepLinkEntryPoint
import com.peto.ramap.deeplink.DeepLinkEvent
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.log.AppAnalytics
import com.peto.ramap.log.analyticsScreenName
import com.peto.ramap.navigation.BackPressController
import com.peto.ramap.navigation.NavigationSource
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.deeplink.ShopDeepLink
import com.peto.ramap.navigation.deeplink.ShopDeepLinkDispatcher
import com.peto.ramap.navigation.deeplink.ShopDeepLinkParser
import com.peto.ramap.notification.NotificationDeepLink
import com.peto.ramap.notification.NotificationDeepLinkParser
import com.peto.ramap.notification.NotificationLaunchDispatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
internal fun AppRouteObservers(
    navigationState: NavigationState,
    toastManager: ToastManager,
    onExitRequested: (() -> Unit)?,
    notificationDeepLinkParser: NotificationDeepLinkParser,
    notificationLaunchDispatcher: NotificationLaunchDispatcher,
    shopDeepLinkParser: ShopDeepLinkParser,
    shopDeepLinkDispatcher: ShopDeepLinkDispatcher,
    deepLinkEntryPoint: DeepLinkEntryPoint,
    appAnalytics: AppAnalytics,
    loginRepository: LoginRepository,
    personalizationStore: PersonalizationStore,
) {
    HandleDeepLinkEvents(
        entryPoint = deepLinkEntryPoint,
        notificationDispatcher = notificationLaunchDispatcher,
        shopDispatcher = shopDeepLinkDispatcher,
        appAnalytics = appAnalytics,
    )

    TrackScreenViews(
        navigationState = navigationState,
        appAnalytics = appAnalytics,
    )

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
    personalizationStore: PersonalizationStore,
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
