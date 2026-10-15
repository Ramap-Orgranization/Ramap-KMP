package com.peto.ramap.ui.shop.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.shop.component.MapInteractionHost
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewSideEffect
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.overview.model.OverviewRouteActions
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.mapNotNull
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OverviewRoute(
    shopId: String,
    onDismissRequest: () -> Unit,
    onShopViewed: (RamenShop) -> Unit,
    onShopLoadFailed: (String) -> Unit,
    onLoginGuideRequested: () -> Unit,
    onSelectedShopHidden: () -> Unit,
    initialShop: RamenShop? = null,
    originSource: AnalyticsSource = AnalyticsSource.MAP,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    viewModel: OverviewViewModel = koinViewModel(),
    content: @Composable (ShopOverviewState, OverviewRouteActions) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoginGuideRequested by rememberUpdatedState(onLoginGuideRequested)
    val currentOnShopViewed by rememberUpdatedState(onShopViewed)
    val currentOnShopLoadFailed by rememberUpdatedState(onShopLoadFailed)
    val currentOnHidden by rememberUpdatedState(onSelectedShopHidden)
    val interactions =
        remember(viewModel) {
            viewModel.sideEffect
                .mapNotNull { effect ->
                    when (effect) {
                        is ShopOverviewSideEffect.Interaction -> {
                            if (effect.effect == ShopInteractionEffect.ShowLoginGuide) currentOnLoginGuideRequested()
                            effect.effect
                        }
                        is ShopOverviewSideEffect.ShopViewed -> {
                            currentOnShopViewed(effect.shop)
                            null
                        }
                        ShopOverviewSideEffect.SelectedShopHidden -> {
                            viewModel.entryContext.advance()
                            currentOnHidden()
                            null
                        }
                        is ShopOverviewSideEffect.LoginCompleted -> null
                    }
                }.buffer(0)
        }

    LaunchedEffect(viewModel, shopId) {
        if (initialShop == null) {
            viewModel.dispatch(ShopOverviewIntent.OnShopIdSelected(shopId))
        } else {
            viewModel.dispatch(ShopOverviewIntent.OnShopSelected(initialShop))
        }
    }

    LaunchedEffect(uiState.hasLoadFailure) {
        if (uiState.hasLoadFailure && uiState.shopOverview == null) currentOnShopLoadFailed(shopId)
    }

    MapInteractionHost(
        sideEffect = interactions,
        isLoggedIn = uiState.isLoggedIn,
        hiddenShopIds = uiState.hiddenShopIds,
        notificationShopIds = uiState.notificationShopIds,
        toastManager = toastManager,
        appSettingsOpener = appSettingsOpener,
        shopShareLinkFactory = shopShareLinkFactory,
        requestNotificationPermission = requestNotificationPermission,
        onNotificationToggled = {
            viewModel.dispatch(ShopOverviewIntent.OnShopNotificationToggled(it, originSource))
        },
        onLoginTypeSelected = { viewModel.dispatch(ShopOverviewIntent.OnLoginTypeSelected(it)) },
        onLoginDismissed = { viewModel.dispatch(ShopOverviewIntent.OnLoginSelectionDismissed) },
    ) { onShopNotificationToggled, requestLoginGuide ->
        content(
            uiState,
            OverviewRouteActions(
                onDismissRequest = {
                    viewModel.entryContext.advance()
                    onDismissRequest()
                },
                onRetry = { viewModel.dispatch(ShopOverviewIntent.OnOverviewRetry) },
                onLoginGuideRequested = requestLoginGuide,
                onBookmarkToggled = { viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(it, originSource)) },
                onShopNotificationToggled = onShopNotificationToggled,
                onHiddenToggled = { viewModel.dispatch(ShopOverviewIntent.OnHiddenToggled(it, originSource)) },
                onShopShareClick = { viewModel.dispatch(ShopOverviewIntent.OnShopShareClicked(it)) },
                onShopMapLinkClick = { shop, provider -> viewModel.dispatch(ShopOverviewIntent.OnShopMapLinkClicked(shop, provider)) },
                onReportSubmit = { wrongFields, description -> viewModel.dispatch(ShopOverviewIntent.OnShopReportSubmitted(wrongFields, description)) },
                onAppleMapsClick = { shop ->
                    viewModel.dispatch(ShopOverviewIntent.OnShopMapLinkClicked(shop, "apple"))
                    ExternalUriOpener.openAppleMaps(
                        name = shop.name,
                        address = shop.address,
                        latitude = shop.location.lat,
                        longitude = shop.location.lng,
                    )
                },
            ),
        )
    }
}
