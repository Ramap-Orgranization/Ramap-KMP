package com.peto.ramap.ui.main.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.RamenShopSummaries
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.wating.toUiModel
import com.peto.ramap.designsystem.shop.ShopDetailContent
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.component.BookmarkedFilterButton
import com.peto.ramap.ui.main.map.component.MenuCategoryFilterRow
import com.peto.ramap.ui.main.map.component.OpenFilterButton
import com.peto.ramap.ui.main.map.component.RecentSearchHistory
import com.peto.ramap.ui.main.map.component.SearchBar
import com.peto.ramap.ui.main.map.component.SearchResultGuide
import com.peto.ramap.ui.main.map.component.SearchResultList
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.model.CameraPosition
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.retry_action

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
    onPlaceSelected: (PlaceSearchResult) -> Unit,
    onShopDetailDismissed: () -> Unit,
    onShopDetailRetry: () -> Unit,
    onRequestedShopDismissed: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onRecentSearchSelected: (String) -> Unit,
    onRecentSearchDeleted: (String) -> Unit,
    onRecentSearchesCleared: () -> Unit,
    onRecentlyViewedShopSelected: (String) -> Unit,
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
    onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    onBookmarkedShopsToggle: () -> Unit,
    showShopDetail: Boolean,
) {
    val selectedShop: RamenShop? = uiState.selectedShop
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var isSearchFocused by remember { mutableStateOf(false) }

    val searchResultSheetState =
        rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
        )
    val searchResultScaffoldState =
        rememberBottomSheetScaffoldState(
            bottomSheetState = searchResultSheetState,
        )

    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )
    val searchBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val searchBarHeight = 52.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val detailBottomSheetMaxHeight = maxHeight - searchBarTopPadding - searchBarHeight

        NavigationBackHandler(
            state = backEventState,
            isBackEnabled = isBackEnabled && (isSearchFocused || uiState.showBottomSheet),
            onBackCompleted = {
                when {
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
            placeFocusLocation = uiState.placeFocusLocation,
            placeFocusRequestKey = uiState.placeFocusRequestKey,
            shouldBootstrapInitialLocationFocus = uiState.shouldBootstrapLocationFocusStatus,
            selectedShopId = uiState.selectedShop?.id,
            cameraPosition = uiState.cameraPosition,
            onMapMoveStarted = {
                if (isImeVisible) focusManager.clearFocus()
            },
            onBoundsChanged = onBoundsChanged,
            onCameraPositionChanged = onCameraPositionChanged,
            onInitialFocusConsumed = onInitialLocationFocusConsumed,
            onSelectedShopFocusConsumed = onSelectedShopFocusConsumed,
            onMyLocationChanged = onMyLocationChanged,
            onShopClick = { onShopSelected(it, false, AnalyticsSource.MARKER) },
            onLocationPermissionBlocked = onLocationPermissionBlocked,
            onCurrentLocationTimeout = onCurrentLocationTimeout,
        )

        Column(
            modifier =
                if (isSearchFocused) {
                    Modifier
                        .fillMaxSize()
                        .background(CommonColor.White)
                        .padding(top = searchBarTopPadding)
                        .padding(horizontal = 10.dp)
                } else {
                    Modifier
                        .padding(top = searchBarTopPadding)
                        .padding(horizontal = 10.dp)
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchBar(
                    query = uiState.search.input,
                    onQueryChange = onQueryChanged,
                    onFocusChanged = { isSearchFocused = it },
                    isSearchMode = isSearchFocused,
                    modifier = Modifier.weight(1f),
                )

                if (!isSearchFocused) {
                    OpenFilterButton(
                        isActive = uiState.filters.isOpenSelected,
                        onClick = onOpenFilterToggled,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    BookmarkedFilterButton(
                        isActive = uiState.isBookmarkedView,
                        onClick = onBookmarkedShopsToggle,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }

            if (isSearchFocused) {
                RecentSearchHistory(
                    searches = uiState.recentSearches,
                    viewedShops = uiState.recentlyViewedShops,
                    onSearchSelected = { query ->
                        focusManager.clearFocus()
                        onRecentSearchSelected(query)
                    },
                    onSearchDeleted = onRecentSearchDeleted,
                    onSearchesCleared = onRecentSearchesCleared,
                    onViewedShopSelected = onRecentlyViewedShopSelected,
                    categoryLabel = { category ->
                        stringResource(CategoryResourceMapper.label(category))
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                )
            } else {
                MenuCategoryFilterRow(
                    selectedFilter = uiState.filters,
                    onCategoryClick = onCategoryFilterToggled,
                )

                if (uiState.hasViewportLoadFailed) {
                    AppButton(
                        text = stringResource(Res.string.retry_action),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        onClick = onViewportLoadRetry,
                    )
                }
            }
        }

        if (!isSearchFocused && uiState.showSearchResults) {
            BottomSheetScaffold(
                scaffoldState = searchResultScaffoldState,
                sheetPeekHeight = BottomSheetDefaults.SheetPeekHeight,
                containerColor = Color.Transparent,
                sheetContainerColor = CommonColor.White,
                sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                sheetContent = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    ) {
                        val searchResultGuide = uiState.searchResultGuide
                        when {
                            uiState.placeSearchResults.isNotEmpty() ->
                                SearchResultList(
                                    places = uiState.placeSearchResults,
                                    onPlaceClick = onPlaceSelected,
                                )

                            searchResultGuide != null ->
                                SearchResultGuide(guide = searchResultGuide)

                            else ->
                                RamenShopSummaries(
                                    shops = uiState.searchResultShops,
                                    onShopClick = {
                                        onShopSelected(
                                            it,
                                            true,
                                            AnalyticsSource.SEARCH_RESULT,
                                        )
                                    },
                                    categoryLabel = { category ->
                                        stringResource(CategoryResourceMapper.label(category))
                                    },
                                )
                        }
                    }
                },
                content = {},
            )
        }

        ShopDetailContent(
            state =
                uiState.shopDetailState,
            visible = showShopDetail,
            isBackEnabled = isBackEnabled,
            maxHeight = detailBottomSheetMaxHeight,
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
            onPhoneClick = { ExternalUriOpener.open("tel:$it") },
            onWaitingClick = ExternalUriOpener::open,
            shouldShowExternalLink = ExternalUriOpener::isSupportedWebUri,
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
            onReportSubmit = onReportSubmit,
        )

        if (uiState.isShopDetailLoading && selectedShop == null) {
            RamenLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (uiState.isSearchLoading) {
            RamenLoadingIndicator(modifier = Modifier.align(Alignment.Center))
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
            onPlaceSelected = {},
            onShopDetailDismissed = {},
            onShopDetailRetry = {},
            onRequestedShopDismissed = {},
            onQueryChanged = {},
            onRecentSearchSelected = {},
            onRecentSearchDeleted = {},
            onRecentSearchesCleared = {},
            onRecentlyViewedShopSelected = {},
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
