package com.peto.ramap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.deeplink.DeepLinkEntryPoint
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.log.AppAnalytics
import com.peto.ramap.navigation.deeplink.ShopDeepLinkDispatcher
import com.peto.ramap.navigation.deeplink.ShopDeepLinkParser
import com.peto.ramap.navigation.rememberNavigationState
import com.peto.ramap.notification.NotificationDeepLinkParser
import com.peto.ramap.notification.NotificationLaunchDispatcher
import com.peto.ramap.ui.main.map.MapViewModel
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMapTabExited
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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
    personalizationStore: PersonalizationStore = koinInject(),
) {
    val mapViewModel = koinViewModel<MapViewModel>()
    val navigationState =
        rememberNavigationState(
            onMapTabExited = { mapViewModel.dispatch(OnMapTabExited) },
        )

    AppRouteObservers(
        navigationState = navigationState,
        toastManager = toastManager,
        onExitRequested = onExitRequested,
        notificationDeepLinkParser = notificationDeepLinkParser,
        notificationLaunchDispatcher = notificationLaunchDispatcher,
        shopDeepLinkParser = shopDeepLinkParser,
        shopDeepLinkDispatcher = shopDeepLinkDispatcher,
        deepLinkEntryPoint = deepLinkEntryPoint,
        appAnalytics = appAnalytics,
        loginRepository = loginRepository,
        personalizationStore = personalizationStore,
    )

    val overlayState =
        rememberAppOverlayState(
            navigationState = navigationState,
            toastManager = toastManager,
        )

    val onLoginTypeSelected: (LoginType) -> Unit = overlayState::signIn

    Box(modifier = Modifier.fillMaxSize()) {
        NavigationGraph(
            navigationState = navigationState,
            toastManager = toastManager,
            hasActiveOverlay = overlayState.isOverlayActive,
            openProfile = overlayState::openProfile,
            onLoginTypeSelected = onLoginTypeSelected,
            onOpenReviewWriteOverlay = { shopId ->
                overlayState.overlayReviewWriteArgs = shopId to null
            },
            onOpenReviewEditOverlay = { shopId, reviewId ->
                overlayState.overlayReviewWriteArgs = shopId to reviewId
            },
        )

        OverlayHost(
            overlayState = overlayState,
            navigationState = navigationState,
            onLoginTypeSelected = onLoginTypeSelected,
        )
    }

    LoginGuideDialog(
        visible = overlayState.showProfileLoginGuide,
        onDismiss = {
            overlayState.showProfileLoginGuide = false
            overlayState.pendingProfileTarget = null
        },
        onLoginTypeSelected = { type ->
            overlayState.showProfileLoginGuide = false
            onLoginTypeSelected(type)
        },
        loginButton = { type, onClick -> LoginButton(type, onClick) },
    )
}
