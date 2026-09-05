package com.peto.ramap.ui.main.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.notice.OperatingNoticeBottomSheet
import com.peto.ramap.designsystem.resource.wating.toUiModel
import com.peto.ramap.designsystem.shop.ShopDetailContent
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.component.ClusterShopList
import com.peto.ramap.ui.main.map.component.SearchContent
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.rememberBalloonBuilder
import com.skydoves.balloon.rememberBalloonState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapContent(
    uiState: MapUiState,
    isBackEnabled: Boolean,
    onBoundsChanged: (MapBounds) -> Unit,
    onCameraPositionChanged: (CameraPosition) -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onCurrentLocationTimeout: () -> Unit,
    onShopSelected: (RamenShop, Boolean, AnalyticsSource) -> Unit,
    onShopDetailDismissed: () -> Unit,
    onShopDetailRetry: () -> Unit,
    onRequestedShopDismissed: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onRecentSearchSelected: (String) -> Unit,
    onRecentSearchDeleted: (String) -> Unit,
    onRecentSearchesCleared: () -> Unit,
    onSearchResultsDismissed: () -> Unit,
    onInitialLocationFocusConsumed: () -> Unit,
    onSelectedShopFocusConsumed: () -> Unit,
    onCategoryFilterToggled: (Category) -> Unit,
    onOpenFilterToggled: () -> Unit,
    onViewportLoadRetry: () -> Unit,
    onBookmarkToggled: (RamenShop) -> Unit,
    onShopNotificationToggled: (RamenShop) -> Unit,
    showNotificationActions: Boolean = true,
    onHiddenToggled: (RamenShop) -> Unit,
    onShopShareClick: (RamenShop) -> Unit,
    onShopMapLinkClick: (RamenShop, String) -> Unit,
    onEventClick: (ShopEvent) -> Unit,
    onOperatingNoticeNavigate: (OperatingNotice) -> Unit = {},
    onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    onBookmarkedShopsToggle: () -> Unit,
    showShopDetail: Boolean,
) {
    val selectedShop: RamenShop? = uiState.selectedShop
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedNotice by remember { mutableStateOf<OperatingNotice?>(null) }
    var overlappingClusterShops by remember { mutableStateOf(emptyList<RamenShop>()) }
    var clusterMenuOffset by remember { mutableStateOf<IntOffset?>(null) }
    val clusterBalloonStyle =
        rememberBalloonBuilder {
            setArrowSize(10.dp)
            setArrowPosition(0.5f)
            setCornerRadius(8.dp)
            setBackgroundColor(CommonColor.White)
            setBorder(GrayColor.C200, 1.dp)
            setPadding(15.dp)
            setMinWidth(210.dp)
            setMaxWidth(310.dp)
            setDismissWhenTouchOutside(true)
            setDismissWhenBackPressed(true)
        }
    val clusterBalloonState = rememberBalloonState(clusterBalloonStyle)

    clusterBalloonState.onDismiss = {
        overlappingClusterShops = emptyList()
        clusterMenuOffset = null
    }

    LaunchedEffect(uiState.markerShops, selectedShop) {
        clusterBalloonState.dismiss()
        overlappingClusterShops = emptyList()
        clusterMenuOffset = null
    }
    LaunchedEffect(overlappingClusterShops, clusterMenuOffset) {
        if (overlappingClusterShops.isNotEmpty()) clusterBalloonState.showAlignTop()
    }

    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        NavigationBackHandler(
            state = backEventState,
            isBackEnabled =
                isBackEnabled && (clusterBalloonState.isVisible || isSearchFocused || uiState.showBottomSheet),
            onBackCompleted = {
                when {
                    clusterBalloonState.isVisible -> clusterBalloonState.dismiss()
                    isSearchFocused -> focusManager.clearFocus()
                    selectedShop != null -> onShopDetailDismissed()
                    uiState.hasShopDetailLoadFailed -> onRequestedShopDismissed()

                    else -> onSearchResultsDismissed()
                }
            },
        )

        RamapMapView(
            modifier = Modifier.fillMaxSize(),
            shops = uiState.markerShops,
            focusShops = uiState.focusShops,
            focusNearestToCurrentLocation = uiState.shouldFocusNearestSearchResult,
            focusRequestKey = uiState.focusRequestKey,
            initialFocusLocation = uiState.initialFocusLocation,
            shouldBootstrapInitialLocationFocus = uiState.shouldBootstrapLocationFocusStatus,
            selectedShopId = uiState.selectedShop?.id,
            cameraPosition = uiState.cameraPosition,
            onMapMoveStarted = {
                clusterBalloonState.dismiss()
                if (isImeVisible) focusManager.clearFocus()
            },
            onBoundsChanged = onBoundsChanged,
            onCameraPositionChanged = onCameraPositionChanged,
            onInitialFocusConsumed = onInitialLocationFocusConsumed,
            onSelectedShopFocusConsumed = onSelectedShopFocusConsumed,
            onMyLocationChanged = onMyLocationChanged,
            onShopClick = {
                clusterBalloonState.dismiss()
                onShopSelected(it, false, AnalyticsSource.MARKER)
            },
            onClusterClick = { shops, offset ->
                overlappingClusterShops = shops
                clusterMenuOffset = offset
            },
            onLocationPermissionBlocked = onLocationPermissionBlocked,
            onCurrentLocationTimeout = onCurrentLocationTimeout,
        )

        if (overlappingClusterShops.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (clusterMenuOffset == null) Alignment.Center else Alignment.TopStart,
            ) {
                Balloon(
                    state = clusterBalloonState,
                    modifier =
                        Modifier
                            .size(1.dp)
                            .then(clusterMenuOffset?.let { Modifier.offset { it } } ?: Modifier),
                    balloonContent = {
                        ClusterShopList(
                            shops = overlappingClusterShops,
                            operatingNotices = uiState.operatingNotices,
                            onShopClick = { shop ->
                                clusterBalloonState.dismiss()
                                onShopSelected(shop, false, AnalyticsSource.MARKER)
                            },
                            modifier =
                                Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                        )
                    },
                ) {}
            }
        }

        SearchContent(
            uiState = uiState,
            isSearchFocused = isSearchFocused,
            maxHeight = maxHeight,
            onSearchFocusChanged = { isSearchFocused = it },
            onQueryChanged = onQueryChanged,
            onRecentSearchSelected = onRecentSearchSelected,
            onRecentSearchDeleted = onRecentSearchDeleted,
            onRecentSearchesCleared = onRecentSearchesCleared,
            onRecentlyViewedShopSelected = { shop ->
                onShopSelected(shop, true, AnalyticsSource.RECENTLY_VIEWED)
            },
            onShopSelected = onShopSelected,
            onCategoryFilterToggled = onCategoryFilterToggled,
            onOpenFilterToggled = onOpenFilterToggled,
            onViewportLoadRetry = onViewportLoadRetry,
            onBookmarkedShopsToggle = onBookmarkedShopsToggle,
        )

        ShopDetailContent(
            state =
                uiState.shopDetailState,
            visible = showShopDetail,
            isBackEnabled = isBackEnabled,
            maxHeight = maxHeight,
            waitingSystem = selectedShop?.let { uiState.shopWaiting[it.id].toUiModel() },
            isBookmarked = selectedShop?.id in uiState.bookmarkedShopIds,
            isNotificationEnabled = selectedShop?.id in uiState.notificationShopIds,
            showNotificationActions = showNotificationActions,
            isHidden = selectedShop?.id in uiState.hiddenShopIds,
            isLoggedIn = uiState.isLoggedIn,
            onDismissRequest = {
                if (selectedShop != null) onShopDetailDismissed() else onRequestedShopDismissed()
            },
            onRetry = onShopDetailRetry,
            onBookmarkToggled = onBookmarkToggled,
            onShopNotificationToggled = onShopNotificationToggled,
            onHiddenToggled = onHiddenToggled,
            onShopShareClick = onShopShareClick,
            onShopMapLinkClick = onShopMapLinkClick,
            onWaitingClick = ExternalUriOpener::open,
            onExternalLinkClick = ExternalUriOpener::open,
            isAppleMapsAvailable = ExternalUriOpener.isAppleMapsAvailable,
            onAppleMapsClick = { shop ->
                onShopMapLinkClick(shop, "apple")
                ExternalUriOpener.openAppleMaps(
                    name = shop.name,
                    address = shop.address,
                    latitude = shop.location.lat,
                    longitude = shop.location.lng,
                )
            },
            onEventClick = onEventClick,
            onOperatingNoticeClick = { selectedNotice = it },
            onReportSubmit = onReportSubmit,
        )

        if ((uiState.isShopDetailLoading && selectedShop == null) || uiState.isSearchLoading) {
            RamenLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        selectedNotice?.let { notice ->
            OperatingNoticeBottomSheet(
                notice = notice,
                isSourceUrlSupported = ExternalUriOpener::isSupportedWebUri,
                onSourceClick = ExternalUriOpener::open,
                onShopClick = {
                    if (it != selectedShop?.id) {
                        onOperatingNoticeNavigate(notice)
                    }
                    selectedNotice = null
                },
                onDismiss = { selectedNotice = null },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapContentPreview(
    @PreviewParameter(RamenShopsPreviewParameterProvider::class) shops: RamenShops,
) {
    RamapTheme {
        MapContent(
            uiState = MapUiState(shops = shops, recentSearches = listOf("멘야 하나비", "라멘 트럭")),
            isBackEnabled = true,
            onBoundsChanged = {},
            onCameraPositionChanged = {},
            onMyLocationChanged = {},
            onLocationPermissionBlocked = {},
            onCurrentLocationTimeout = {},
            onShopSelected = { _, _, _ -> },
            onShopDetailDismissed = {},
            onShopDetailRetry = {},
            onRequestedShopDismissed = {},
            onQueryChanged = {},
            onRecentSearchSelected = {},
            onRecentSearchDeleted = {},
            onRecentSearchesCleared = {},
            onSearchResultsDismissed = {},
            onInitialLocationFocusConsumed = {},
            onSelectedShopFocusConsumed = {},
            onCategoryFilterToggled = {},
            onOpenFilterToggled = {},
            onViewportLoadRetry = {},
            onBookmarkToggled = {},
            onShopNotificationToggled = {},
            onHiddenToggled = {},
            onShopShareClick = {},
            onShopMapLinkClick = { _, _ -> },
            onEventClick = {},
            onReportSubmit = { _, _ -> },
            onBookmarkedShopsToggle = {},
            showShopDetail = false,
        )
    }
}
