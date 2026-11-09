package com.peto.ramap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.peto.ramap.deeplink.DeepLinkEntryPoint
import com.peto.ramap.deeplink.DeepLinkEvent
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginSessionState
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
import com.peto.ramap.notification.NotificationLaunchDispatcher
import com.peto.ramap.platform.ExternalUriOpener
import kotlinx.coroutines.flow.filterNotNull

@Composable
internal fun AppRouteObservers(
    navigationState: NavigationState,
    toastManager: ToastManager,
    onExitRequested: (() -> Unit)?,
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

    LaunchedEffect(loginRepository, navigationState) {
        observeArchiveSession(loginRepository, navigationState)
    }

    HandleNotificationDeepLink(
        navigationState = navigationState,
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

internal suspend fun observeArchiveSession(
    loginRepository: LoginRepository,
    navigationState: NavigationState,
) {
    loginRepository.session.collect { session ->
        if (session == LoginSessionState.NOT_AUTHENTICATED) navigationState.clearArchiveMap()
    }
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
                is DeepLinkEvent.Notification -> notificationDispatcher.dispatch(event.value, event.notificationId)
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
            val route = navigationState.currentRoute
            route to route.analyticsScreenName(navigationState.selectedNewsTab)
        }.collect { (_, screenName) ->
            appAnalytics.logScreenView(screenName)
        }
    }
}

@Composable
private fun TrackUserProperties(
    appAnalytics: AppAnalytics,
    loginRepository: LoginRepository,
    personalizationStore: PersonalizationStore,
) {
    LaunchedEffect(appAnalytics, loginRepository) {
        appAnalytics.clearLegacyHiddenCount()
        loginRepository.session.collect { state ->
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
    notificationLaunchDispatcher: NotificationLaunchDispatcher,
    appAnalytics: AppAnalytics,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(
        navigationState,
        notificationLaunchDispatcher,
        lifecycleOwner,
    ) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notificationLaunchDispatcher.pendingDeepLink.collect { value ->
                if (value == null) return@collect

                when (val destination = value.destination) {
                    is NotificationDeepLink.Event -> {
                        appAnalytics.logNotificationOpened(eventId = destination.eventId)
                        navigationState.showEvent(destination.eventId)
                    }
                    is NotificationDeepLink.OperatingNotice -> navigationState.showOperatingNotices(destination.noticeId)
                    NotificationDeepLink.OperatingNotices -> navigationState.showOperatingNotices()
                    is NotificationDeepLink.ExternalUrl -> ExternalUriOpener.open(destination.url)
                    NotificationDeepLink.None -> Unit
                }

                notificationLaunchDispatcher.consume(value)
            }
        }
    }
}
