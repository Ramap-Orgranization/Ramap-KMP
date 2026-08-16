package com.peto.ramap.ui.main.map

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.resource.wating.toUiModel
import com.peto.ramap.designsystem.shop.ShopDetailContent
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.main.map.contract.MapIntent
import org.koin.compose.koinInject

@Composable
fun ShopDetailHost(
    shopId: String,
    viewModel: MapViewModel,
    onDismiss: () -> Unit,
    onShowOnMap: (String) -> Unit,
    onEventNavigate: (ShopEvent) -> Unit = {},
    originSource: AnalyticsSource = AnalyticsSource.MAP,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(shopId) {
        viewModel.dispatch(MapIntent.OnShopIdSelected(shopId))
    }

    MapInteractionHost(
        sideEffect = viewModel.sideEffect,
        isLoggedIn = uiState.isLoggedIn,
        hiddenShopIds = uiState.hiddenShopIds,
        notificationShopIds = uiState.notificationShopIds,
        toastManager = toastManager,
        appSettingsOpener = appSettingsOpener,
        shopShareLinkFactory = shopShareLinkFactory,
        requestNotificationPermission = requestNotificationPermission,
        onNotificationToggled = {
            viewModel.dispatch(MapIntent.OnShopNotificationToggled(it, originSource))
        },
        onLoginTypeSelected = { viewModel.dispatch(MapIntent.OnLoginTypeSelected(it)) },
        onLoginDismissed = { viewModel.dispatch(MapIntent.OnLoginSelectionDismissed) },
    ) { onShopNotificationToggled ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            ShopDetailContent(
                state = uiState.shopDetailState,
                isBackEnabled = true,
                maxHeight = maxHeight,
                showRequestedLoadingInSheet = true,
                waitingSystem = uiState.selectedShop?.let { uiState.shopWaiting[it.id].toUiModel() },
                isBookmarked = uiState.selectedShop?.id in uiState.bookmarkedShopIds,
                isNotificationEnabled = uiState.selectedShop?.id in uiState.notificationShopIds,
                showNotificationActions = NotificationPermissionRequester.isSupported,
                isHidden = uiState.selectedShop?.id in uiState.hiddenShopIds,
                isLoggedIn = uiState.isLoggedIn,
                onDismissRequest = {
                    viewModel.dispatch(MapIntent.OnShopDetailDismissed)
                    onDismiss()
                },
                onRetry = { viewModel.dispatch(MapIntent.OnShopDetailRetry) },
                onBookmarkToggled = {
                    viewModel.dispatch(MapIntent.OnBookmarkToggled(it, originSource))
                },
                onShopNotificationToggled = onShopNotificationToggled,
                onHiddenToggled = {
                    viewModel.dispatch(MapIntent.OnHiddenToggled(it, originSource))
                },
                onShopShareClick = { viewModel.dispatch(MapIntent.OnShopShareClicked(it)) },
                onShopMapLinkClick = { shop, provider ->
                    viewModel.dispatch(MapIntent.OnShopMapLinkClicked(shop, provider))
                },
                onPhoneClick = { ExternalUriOpener.open("tel:$it") },
                onWaitingClick = ExternalUriOpener::open,
                shouldShowExternalLink = ExternalUriOpener::isSupportedWebUri,
                onExternalLinkClick = ExternalUriOpener::open,
                isAppleMapsAvailable = ExternalUriOpener.isAppleMapsAvailable,
                onAppleMapsClick = { shop ->
                    viewModel.dispatch(MapIntent.OnShopMapLinkClicked(shop, "apple"))
                    ExternalUriOpener.openAppleMaps(
                        name = shop.name,
                        address = shop.address,
                        latitude = shop.location.lat,
                        longitude = shop.location.lng,
                    )
                },
                onEventClick = onEventNavigate,
                onReportSubmit = { wrongFields, description ->
                    viewModel.dispatch(MapIntent.OnShopReportSubmitted(wrongFields, description))
                },
                onShowOnMap = { selectedShopId ->
                    viewModel.dispatch(MapIntent.OnShopDetailDismissed)
                    onShowOnMap(selectedShopId)
                },
            )
        }
    }
}
