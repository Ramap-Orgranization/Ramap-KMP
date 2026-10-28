package com.peto.ramap.ui.shop.component

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.peto.ramap.designsystem.button.AppExtendedFloatingActionButton
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailDisplayState
import com.peto.ramap.ui.shop.model.ShopDetailSheetConfig
import com.peto.ramap.ui.shop.model.ShopOverviewUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_map_outline
import ramap.shared.generated.resources.ranking_show_shop_on_map

@Composable
fun ShopDetailContent(
    uiState: ShopDetailContentUiState,
    config: ShopDetailSheetConfig,
    actions: ShopDetailActions,
    modifier: Modifier = Modifier,
) {
    ShopDetailBody(
        uiState = uiState,
        actions = actions,
        visible = config.visible,
        showReviewsOnOpen = config.showReviewsOnOpen,
    ) { items, loadMoreState, loadMoreEnabled ->
        val listState = key(uiState.shopId) { rememberLazyListState() }
        val state = uiState.sheetState
        val mapAction =
            if (uiState.displayState == ShopDetailDisplayState.Content && state is ShopOverviewUiState.Content && actions.externalLinkActions.onShowOnMap != null) {
                { actions.externalLinkActions.onShowOnMap.invoke(state.overview.shop.id) }
            } else {
                null
            }
        ShopDetailBottomSheet(
            shopId = uiState.shopId,
            listState = listState,
            onDismissRequest = actions.onDismissRequest,
            isBackEnabled = config.isBackEnabled,
            maxHeight = config.maxHeight,
            isNavigationBarPadded = config.isNavigationBarPadded,
            loadMoreState = loadMoreState,
            loadMoreEnabled = loadMoreEnabled,
            onLoadMore = actions.reviewActions.onLoadMoreReviews,
            modifier = modifier,
            floatingAction =
                mapAction?.let { showOnMap ->
                    {
                        AppExtendedFloatingActionButton(
                            text = stringResource(Res.string.ranking_show_shop_on_map),
                            icon = Res.drawable.ic_map_outline,
                            onClick = showOnMap,
                        )
                    }
                },
            content = items,
        )
    }
}
