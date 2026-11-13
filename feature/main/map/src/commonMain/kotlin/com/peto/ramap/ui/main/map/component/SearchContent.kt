package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.button.BookmarkedFilterButton
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.ShopHoursFilter
import com.peto.ramap.theme.SurfaceColor
import com.peto.ramap.ui.main.map.contract.MapUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.recommendation_sheet_drag_handle
import ramap.shared.generated.resources.retry_action
import kotlin.math.roundToInt

private val MapFilterButtonSize = 46.dp
private val RecommendationHandlePadding = 22.dp
private val RecommendationHandleBarHeight = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchContent(
    uiState: MapUiState,
    isSearchFocused: Boolean,
    isBackEnabled: Boolean,
    maxHeight: Dp,
    onSearchFocusChanged: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    onPlaceSelected: (PlaceSearchResult) -> Unit,
    onRecentSearchSelected: (String) -> Unit,
    onRecentSearchDeleted: (String) -> Unit,
    onRecentSearchesCleared: () -> Unit,
    onRecentlyViewedShopSelected: (RamenShop) -> Unit,
    onShopSelected: (RamenShop, Boolean, AnalyticsSource) -> Unit,
    onCategoryFilterToggled: (Category) -> Unit,
    onHoursFilterOpened: () -> Unit,
    onHoursFilterApplied: (ShopHoursFilter) -> Unit,
    onRecommendationToggled: () -> Unit,
    onRecommendationLoadRetry: () -> Unit,
    onViewportLoadRetry: () -> Unit,
    onBookmarkedShopsToggle: () -> Unit,
    onBookmarkClick: (RamenShop, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var mapControlsHeightPx by remember { mutableIntStateOf(0) }
    val recommendationListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showRecommendationList = !isSearchFocused && uiState.showRecommendationList
    val recommendationSheetState =
        key(uiState.recommendationToggleKey) {
            rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
        }
    val searchResultSheetState =
        rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
        )
    val searchResultScaffoldState =
        rememberBottomSheetScaffoldState(
            bottomSheetState = if (showRecommendationList) recommendationSheetState else searchResultSheetState,
        )

    NavigationBackHandler(
        state = rememberNavigationEventState<NavigationEventInfo>(currentInfo = NavigationEventInfo.None),
        isBackEnabled =
            isBackEnabled &&
                (
                    (showRecommendationList && recommendationSheetState.currentValue == SheetValue.Expanded) ||
                        (!showRecommendationList && uiState.showSearchResults && searchResultSheetState.currentValue == SheetValue.Expanded)
                ),
        onBackCompleted = {
            coroutineScope.launch {
                if (showRecommendationList) {
                    recommendationSheetState.partialExpand()
                } else {
                    searchResultSheetState.partialExpand()
                }
            }
        },
    )

    LaunchedEffect(
        isSearchFocused,
        uiState.search.focusRequestKey,
        uiState.showSearchResults,
    ) {
        if (!isSearchFocused && uiState.showSearchResults) {
            searchResultSheetState.partialExpand()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(
                    if (isSearchFocused) {
                        Modifier.background(SurfaceColor.Background)
                    } else {
                        Modifier
                    },
                ),
    ) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        Column(
            modifier =
                Modifier
                    .onSizeChanged { size ->
                        if (!isSearchFocused) mapControlsHeightPx = size.height
                    }.padding(top = statusBarPadding)
                    .padding(horizontal = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchBar(
                    query = uiState.search.input,
                    onQueryChange = onQueryChanged,
                    onFocusChanged = onSearchFocusChanged,
                    isSearchMode = isSearchFocused,
                    resetKey = uiState.recommendationToggleKey,
                    modifier = Modifier.weight(1f),
                )

                if (!isSearchFocused) {
                    val filterButtonModifier = Modifier.size(MapFilterButtonSize)
                    RecommendationFilterButton(
                        isActive = uiState.isRecommendationEnabled,
                        onClick = onRecommendationToggled,
                        modifier = filterButtonModifier,
                    )
                    BookmarkedFilterButton(
                        isActive = uiState.isBookmarkedView,
                        onClick = onBookmarkedShopsToggle,
                        modifier = filterButtonModifier,
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
                    onViewedShopSelected = { shop ->
                        focusManager.clearFocus()
                        onRecentlyViewedShopSelected(shop)
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                )
            } else {
                MenuCategoryFilterRow(
                    selectedFilter = uiState.filters,
                    now = uiState.currentDateTime,
                    hoursFilterResultCount = { uiState.hoursFilterResultCount(it) },
                    onHoursFilterOpened = onHoursFilterOpened,
                    onHoursFilterApplied = onHoursFilterApplied,
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

        if (!isSearchFocused && (uiState.showSearchResults || showRecommendationList)) {
            val showsPlaceShortcut = !showRecommendationList && uiState.hasSearchResult && uiState.search.places.isNotEmpty()
            val sheetContentHeight = (maxHeight - statusBarPadding - 116.dp).coerceAtLeast(88.dp)
            val mapControlsHeight = with(density) { mapControlsHeightPx.toDp() }
            val recommendationContentHeight =
                (maxHeight - mapControlsHeight - 8.dp - RecommendationHandlePadding - RecommendationHandleBarHeight).coerceAtLeast(110.dp)
            val searchResultPeekHeight = (maxHeight / 2).coerceAtLeast(140.dp)

            BottomSheetScaffold(
                scaffoldState = searchResultScaffoldState,
                sheetPeekHeight = if (showRecommendationList) 142.dp - RecommendationHandlePadding else searchResultPeekHeight,
                containerColor = Color.Transparent,
                sheetContainerColor = SurfaceColor.Background,
                sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                sheetDragHandle = {
                    if (showRecommendationList) {
                        RecommendationSheetDragHandle()
                    } else {
                        BottomSheetDefaults.DragHandle()
                    }
                },
                sheetContent = {
                    if (showRecommendationList) {
                        RecommendationList(
                            shops = uiState.recommendationListShops,
                            bookmarkedShopIds = uiState.bookmarkedShopIds,
                            bookmarkUpdatingShopIds = uiState.bookmarkUpdatingShopIds,
                            isLoading = uiState.isRecommendationLoading,
                            hasLoadFailed = uiState.recommendedShops == null && !uiState.isRecommendationLoading,
                            listState = recommendationListState,
                            onRetry = onRecommendationLoadRetry,
                            onShopClick = { onShopSelected(it, true, AnalyticsSource.SEARCH_RESULT) },
                            onBookmarkClick = onBookmarkClick,
                            modifier = Modifier.height(recommendationContentHeight),
                        )
                    } else {
                        Column(
                            modifier =
                                Modifier
                                    .height(sheetContentHeight)
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            if (uiState.hasSearchResult) {
                                SearchResultList(
                                    shops = uiState.searchResultShops,
                                    operatingNotices = uiState.operatingNotices,
                                    currentDateTime = uiState.currentDateTime,
                                    isVisitTimeSelected = uiState.isVisitTimeSelected,
                                    onShopClick = { onShopSelected(it, true, AnalyticsSource.SEARCH_RESULT) },
                                )
                            }
                            if (!showsPlaceShortcut && uiState.search.places.isNotEmpty()) {
                                PlaceDestinationList(
                                    places = uiState.search.places,
                                    onPlaceSelected = onPlaceSelected,
                                )
                            }
                            if (!uiState.hasSearchResult && uiState.search.places.isEmpty()) {
                                uiState.searchResultGuide?.let { guide -> SearchResultGuide(guide = guide) }
                            }
                        }
                    }
                },
                content = {},
            )

            if (showsPlaceShortcut) {
                PlaceDestinationShortcut(
                    places = uiState.search.places,
                    onPlaceSelected = onPlaceSelected,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = searchResultSheetState.requireOffset().roundToInt(),
                                )
                            }.graphicsLayer {
                                translationY = -size.height - 10.dp.toPx()
                            },
                )
            }
        }
    }
}

@Composable
private fun RecommendationSheetDragHandle() {
    val description = stringResource(Res.string.recommendation_sheet_drag_handle)

    Box(
        modifier =
            Modifier
                .padding(top = RecommendationHandlePadding)
                .size(width = 32.dp, height = RecommendationHandleBarHeight)
                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                .semantics { contentDescription = description },
    )
}
