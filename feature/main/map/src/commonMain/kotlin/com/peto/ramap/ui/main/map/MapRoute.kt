package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCameraPositionChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnHiddenToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLocationPermissionBlocked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLoginSelectionDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLoginTypeSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnOpenFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchDeleted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchesCleared
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRequestedShopDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSearchResultsDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSelectedShopFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailRetry
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopIdSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopMapLinkClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopNotificationToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopShareClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnViewportLoadRetry
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.current_location_timeout_message

@Composable
fun MapRoute(
    isBackEnabled: Boolean = true,
    onDetailDismissed: () -> Unit = {},
    onEventNavigate: (ShopEvent) -> Unit = {},
    onOperatingNoticeNavigate: (OperatingNotice) -> Unit,
    requestedShopId: String? = null,
    showShopDetail: Boolean = true,
    originSource: AnalyticsSource = AnalyticsSource.MAP,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    viewModel: MapViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var shouldShowShopDetail by remember(requestedShopId, showShopDetail) { mutableStateOf(showShopDetail) }
    var detailSource by remember(requestedShopId, originSource) { mutableStateOf(originSource) }

    LaunchedEffect(requestedShopId) {
        if (requestedShopId == null) {
            viewModel.dispatch(OnRequestedShopDismissed)
        } else {
            viewModel.dispatch(OnShopIdSelected(requestedShopId))
        }
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
        onNotificationToggled = { viewModel.dispatch(OnShopNotificationToggled(it, detailSource)) },
        onLoginTypeSelected = { viewModel.dispatch(OnLoginTypeSelected(it)) },
        onLoginDismissed = { viewModel.dispatch(OnLoginSelectionDismissed) },
    ) { onShopNotificationToggled ->
        MapContent(
            uiState = uiState,
            showNotificationActions = NotificationPermissionRequester.isSupported,
            isBackEnabled = isBackEnabled,
            onBoundsChanged = { viewModel.dispatch(OnBoundsChanged(it)) },
            onCameraPositionChanged = { viewModel.dispatch(OnCameraPositionChanged(it)) },
            onMyLocationChanged = { viewModel.dispatch(OnMyLocationChanged(it)) },
            onLocationPermissionBlocked = { viewModel.dispatch(OnLocationPermissionBlocked) },
            onCurrentLocationTimeout = {
                coroutineScope.launch {
                    toastManager.show(
                        ToastData(
                            message = Res.string.current_location_timeout_message,
                            type = ToastType.DEFAULT,
                        ),
                    )
                }
            },
            onShopSelected = { shop, shouldFocus, source ->
                shouldShowShopDetail = true
                detailSource = AnalyticsSource.MAP
                viewModel.dispatch(OnShopSelected(shop, shouldFocus, source))
            },
            onShopDetailDismissed = {
                detailSource = AnalyticsSource.MAP
                viewModel.dispatch(OnShopDetailDismissed)
                onDetailDismissed()
            },
            onShopDetailRetry = {
                if (uiState.selectedShop != null) {
                    viewModel.dispatch(OnShopDetailRetry)
                } else {
                    requestedShopId?.let { shopId -> viewModel.dispatch(OnShopIdSelected(shopId)) }
                }
            },
            onRequestedShopDismissed = {
                viewModel.dispatch(OnRequestedShopDismissed)
                onDetailDismissed()
            },
            onQueryChanged = { viewModel.dispatch(OnQueryChanged(it)) },
            onRecentSearchSelected = { viewModel.dispatch(OnRecentSearchSelected(it)) },
            onRecentSearchDeleted = { viewModel.dispatch(OnRecentSearchDeleted(it)) },
            onRecentSearchesCleared = { viewModel.dispatch(OnRecentSearchesCleared) },
            onSearchResultsDismissed = { viewModel.dispatch(OnSearchResultsDismissed) },
            onInitialLocationFocusConsumed = { viewModel.dispatch(OnInitialLocationFocusConsumed) },
            onSelectedShopFocusConsumed = { viewModel.dispatch(OnSelectedShopFocusConsumed) },
            onCategoryFilterToggled = { viewModel.dispatch(OnCategoryFilterToggled(it)) },
            onOpenFilterToggled = { viewModel.dispatch(OnOpenFilterToggled) },
            onViewportLoadRetry = { viewModel.dispatch(OnViewportLoadRetry) },
            onBookmarkToggled = { viewModel.dispatch(OnBookmarkToggled(it, detailSource)) },
            onShopNotificationToggled = onShopNotificationToggled,
            onHiddenToggled = { viewModel.dispatch(OnHiddenToggled(it, detailSource)) },
            onShopShareClick = { viewModel.dispatch(OnShopShareClicked(it)) },
            onShopMapLinkClick = { shop, provider -> viewModel.dispatch(OnShopMapLinkClicked(shop, provider)) },
            onReportSubmit = { wrongFields, description ->
                viewModel.dispatch(OnShopReportSubmitted(wrongFields, description))
            },
            onBookmarkedShopsToggle = { viewModel.dispatch(OnBookmarkedShopsToggled) },
            onEventClick = onEventNavigate,
            onOperatingNoticeNavigate = onOperatingNoticeNavigate,
            showShopDetail = shouldShowShopDetail,
        )
    }
}
