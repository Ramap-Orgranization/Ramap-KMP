package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import com.peto.ramap.ui.login.rememberLoginGuideRequest
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCameraPositionChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLocationPermissionBlocked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLoginSelectionDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLoginTypeSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnOpenFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnOperatingNoticesRefreshRequested
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchDeleted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnRecentSearchesCleared
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSearchResultsDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSelectedShopFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapIntent.OnStatusTimeRefreshed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnViewportLoadRetry
import com.peto.ramap.ui.main.map.contract.MapSideEffect
import com.peto.ramap.ui.main.map.model.ShopDetailRequest
import com.peto.ramap.ui.refresh.BusinessHoursStatusRefreshEffect
import com.peto.ramap.ui.refresh.PeriodicRefreshEffect
import com.peto.ramap.ui.shop.component.MapInteractionHost
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.current_location_timeout_message
import ramap.shared.generated.resources.login_required_visited_shops_description

@Composable
fun MapRoute(
    onReviewNavigate: (String) -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onEditReview: (String, String) -> Unit = { _, _ -> },
    isBackEnabled: Boolean = true,
    onDetailDismissed: () -> Unit = {},
    onEventNavigate: (ShopEvent) -> Unit = {},
    onOperatingNoticeNavigate: (OperatingNotice) -> Unit,
    requestedShopId: String? = null,
    showShopDetail: Boolean = true,
    showReviewsOnOpen: Boolean = false,
    originSource: AnalyticsSource = AnalyticsSource.MAP,
    isUncovered: Boolean = true,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    viewModel: MapViewModel =
        org.koin.compose.viewmodel
            .koinViewModel(),
    onArchiveNavigate: () -> Unit,
    detailContent: @Composable (ShopDetailRequest) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedShopFocusRequest = uiState.selectedShopFocusRequest
    val placeFocusRequest = uiState.placeFocusRequest
    val coroutineScope = rememberCoroutineScope()
    val currentOnArchiveNavigate by rememberUpdatedState(onArchiveNavigate)
    val requestArchiveLogin =
        rememberLoginGuideRequest(
            onLoginTypeSelected = { viewModel.dispatch(MapIntent.OnArchiveLoginTypeSelected(it)) },
            ownerKey = viewModel,
        )
    var shouldShowShopDetail by remember(requestedShopId, showShopDetail) { mutableStateOf(showShopDetail) }
    var shouldShowReviews by remember(requestedShopId, showReviewsOnOpen) { mutableStateOf(showReviewsOnOpen) }
    var detailSource by remember(requestedShopId, originSource) { mutableStateOf(originSource) }

    LaunchedEffect(requestedShopId, showShopDetail) {
        requestedShopId?.let { viewModel.dispatch(MapIntent.OnShopIdSelected(it, showShopDetail)) }
    }
    androidx.compose.runtime.DisposableEffect(viewModel) {
        onDispose { viewModel.dispatch(MapIntent.OnMapTabExited) }
    }
    BusinessHoursStatusRefreshEffect(currentDateTime = uiState.currentDateTime, notices = uiState.operatingNotices) {
        viewModel.dispatch(OnStatusTimeRefreshed)
    }
    PeriodicRefreshEffect(intervalMillis = OPERATING_NOTICE_REFRESH_MILLIS) {
        viewModel.dispatch(OnOperatingNoticesRefreshRequested)
    }

    MapInteractionHost(
        sideEffect =
            remember(viewModel) {
                viewModel.sideEffect.mapNotNull {
                    when (it) {
                        is MapSideEffect.Interaction -> it.effect
                        MapSideEffect.NavigateToArchive -> {
                            currentOnArchiveNavigate()
                            null
                        }
                    }
                }
            },
        isLoggedIn = uiState.isLoggedIn,
        hiddenShopIds = uiState.hiddenShopIds,
        notificationShopIds = uiState.notificationShopIds,
        toastManager = toastManager,
        appSettingsOpener = appSettingsOpener,
        shopShareLinkFactory = shopShareLinkFactory,
        requestNotificationPermission = requestNotificationPermission,
        onNotificationToggled = {},
        onLoginTypeSelected = { viewModel.dispatch(OnLoginTypeSelected(it)) },
        onLoginDismissed = { viewModel.dispatch(OnLoginSelectionDismissed) },
    ) { _, _ ->
        MapContent(
            uiState = uiState,
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
                shouldShowReviews = false
                detailSource = AnalyticsSource.MAP
                viewModel.dispatch(OnShopSelected(shop, shouldFocus, source))
            },
            onShopDetailDismissed = {
                detailSource = AnalyticsSource.MAP
                shouldShowReviews = false
                viewModel.dispatch(OnShopDetailDismissed)
                onDetailDismissed()
            },
            onQueryChanged = { viewModel.dispatch(OnQueryChanged(it)) },
            onPlaceSelected = { viewModel.dispatch(MapIntent.OnPlaceSelected(it)) },
            onPlaceFocusConsumed = { placeFocusRequest?.let { viewModel.dispatch(MapIntent.OnPlaceFocusConsumed(it)) } },
            onRecentSearchSelected = { viewModel.dispatch(OnRecentSearchSelected(it)) },
            onRecentSearchDeleted = { viewModel.dispatch(OnRecentSearchDeleted(it)) },
            onRecentSearchesCleared = { viewModel.dispatch(OnRecentSearchesCleared) },
            onSearchResultsDismissed = { viewModel.dispatch(OnSearchResultsDismissed) },
            onInitialLocationFocusConsumed = { viewModel.dispatch(OnInitialLocationFocusConsumed) },
            onSelectedShopFocusConsumed = {
                selectedShopFocusRequest?.let { viewModel.dispatch(OnSelectedShopFocusConsumed(it)) }
            },
            onCategoryFilterToggled = { viewModel.dispatch(OnCategoryFilterToggled(it)) },
            onOpenFilterToggled = { viewModel.dispatch(OnOpenFilterToggled) },
            onViewportLoadRetry = { viewModel.dispatch(OnViewportLoadRetry) },
            onBookmarkedShopsToggle = { viewModel.dispatch(OnBookmarkedShopsToggled) },
            onArchiveNavigate = {
                if (uiState.isLoggedIn) {
                    onArchiveNavigate()
                } else {
                    requestArchiveLogin(Res.string.login_required_visited_shops_description)
                }
            },
        )
    }
    if (shouldShowShopDetail) {
        uiState.selectedShopId?.let { shopId ->
            key(uiState.shopSelectionKey) {
                detailContent(
                    ShopDetailRequest(
                        shopId = shopId,
                        initialShop = uiState.selectedShop,
                        onDismiss = {
                            shouldShowReviews = false
                            detailSource = AnalyticsSource.MAP
                            viewModel.dispatch(OnShopDetailDismissed)
                            onDetailDismissed()
                        },
                        onSelectedShopHidden = {
                            viewModel.dispatch(MapIntent.OnSelectedShopHidden)
                            onDetailDismissed()
                        },
                        onShopLoadFailed = { viewModel.dispatch(MapIntent.OnShopLoadFailed(it)) },
                        onLoginGuideRequested = { viewModel.dispatch(OnLoginSelectionDismissed) },
                        onShopViewed = { viewModel.dispatch(MapIntent.OnShopViewed(it)) },
                        onShowOnMap = null,
                        onReviewNavigate = onReviewNavigate,
                        onOpenProfile = onOpenProfile,
                        onEditReview = onEditReview,
                        onEventNavigate = onEventNavigate,
                        onOperatingNoticeNavigate = onOperatingNoticeNavigate,
                        originSource = detailSource,
                        isUncovered = isUncovered,
                        isBackEnabled = isBackEnabled,
                        showReviewsOnOpen = shouldShowReviews,
                        toastManager = toastManager,
                        appSettingsOpener = appSettingsOpener,
                        shopShareLinkFactory = shopShareLinkFactory,
                        requestNotificationPermission = requestNotificationPermission,
                    ),
                )
            }
        }
    }
}

private const val OPERATING_NOTICE_REFRESH_MILLIS = 60_000L
