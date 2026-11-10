package com.peto.ramap.ui.shop.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.overview.model.OverviewRouteActions
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OverviewRoute(
    shopId: String,
    onDismissRequest: () -> Unit,
    onShopViewed: (RamenShop) -> Unit,
    onShopLoadFailed: (String) -> Unit,
    onLoginGuideRequested: () -> Unit,
    initialShop: RamenShop? = null,
    originSource: AnalyticsSource = AnalyticsSource.MAP,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    isNotificationPermissionGranted: suspend () -> Boolean = NotificationPermissionRequester::isGranted,
    viewModel: OverviewViewModel = koinViewModel(),
    content: @Composable (ShopOverviewState, OverviewRouteActions) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnShopLoadFailed by rememberUpdatedState(onShopLoadFailed)

    DisposableEffect(viewModel, shopId) {
        onDispose { viewModel.dispatch(ShopOverviewIntent.OnOverviewDismissed) }
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

    OverviewInteractions(
        uiState = uiState,
        viewModel = viewModel,
        originSource = originSource,
        toastManager = toastManager,
        appSettingsOpener = appSettingsOpener,
        shopShareLinkFactory = shopShareLinkFactory,
        requestNotificationPermission = requestNotificationPermission,
        isNotificationPermissionGranted = isNotificationPermissionGranted,
        onLoginGuideRequested = onLoginGuideRequested,
        onShopViewed = onShopViewed,
    ) { onShopNotificationToggled, requestLoginGuide ->
        content(
            uiState,
            OverviewRouteActions(
                onDismissRequest = {
                    viewModel.dispatch(ShopOverviewIntent.OnOverviewDismissed)
                    onDismissRequest()
                },
                onRetry = { viewModel.dispatch(ShopOverviewIntent.OnOverviewRetry) },
                onLoginGuideRequested = requestLoginGuide,
                onBookmarkToggled = { viewModel.dispatch(ShopOverviewIntent.OnBookmarkToggled(it, originSource)) },
                onShopNotificationToggled = onShopNotificationToggled,
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
