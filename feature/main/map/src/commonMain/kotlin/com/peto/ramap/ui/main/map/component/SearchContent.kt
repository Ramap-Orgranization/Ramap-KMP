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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.ui.main.map.contract.MapUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.retry_action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchContent(
    uiState: MapUiState,
    isSearchFocused: Boolean,
    maxHeight: Dp,
    onSearchFocusChanged: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    onRecentSearchSelected: (String) -> Unit,
    onRecentSearchDeleted: (String) -> Unit,
    onRecentSearchesCleared: () -> Unit,
    onRecentlyViewedShopSelected: (RamenShop) -> Unit,
    onShopSelected: (RamenShop, Boolean, AnalyticsSource) -> Unit,
    onCategoryFilterToggled: (Category) -> Unit,
    onOpenFilterToggled: () -> Unit,
    onViewportLoadRetry: () -> Unit,
    onBookmarkedShopsToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val searchResultSheetState =
        rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded,
        )
    val searchResultScaffoldState =
        rememberBottomSheetScaffoldState(
            bottomSheetState = searchResultSheetState,
        )

    LaunchedEffect(
        isSearchFocused,
        uiState.search.focusRequestKey,
        uiState.showSearchResults,
    ) {
        if (!isSearchFocused && uiState.showSearchResults) {
            searchResultSheetState.expand()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(
                    if (isSearchFocused) {
                        Modifier.background(CommonColor.White)
                    } else {
                        Modifier
                    },
                ),
    ) {
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        Column(
            modifier =
                Modifier
                    .padding(top = statusBarPadding)
                    .padding(horizontal = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchBar(
                    query = uiState.search.input,
                    onQueryChange = onQueryChanged,
                    onFocusChanged = onSearchFocusChanged,
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
                    onViewedShopSelected = { shop ->
                        focusManager.clearFocus()
                        onRecentlyViewedShopSelected(shop)
                    },
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
                sheetPeekHeight = BottomSheetDefaults.SheetPeekHeight - 10.dp,
                containerColor = Color.Transparent,
                sheetContainerColor = CommonColor.White,
                sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                sheetContent = {
                    Column(
                        modifier =
                            Modifier
                                .height(maxHeight * 0.5f - BottomSheetDefaults.SheetPeekHeight)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        if (uiState.hasSearchResult) {
                            SearchResultList(
                                shops = uiState.searchResultShops,
                                operatingNotices = uiState.operatingNotices,
                                onShopClick = {
                                    onShopSelected(it, true, AnalyticsSource.SEARCH_RESULT)
                                },
                            )
                        } else {
                            uiState.searchResultGuide?.let { guide ->
                                SearchResultGuide(guide = guide)
                            }
                        }
                    }
                },
                content = {},
            )
        }
    }
}
