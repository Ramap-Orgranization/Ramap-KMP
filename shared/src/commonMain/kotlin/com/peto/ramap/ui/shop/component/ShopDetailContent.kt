package com.peto.ramap.ui.shop.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppExtendedFloatingActionButton
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.shop.component.dialog.ReportDialog
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailDisplayState
import com.peto.ramap.ui.shop.model.ShopDetailSheetConfig
import com.peto.ramap.ui.shop.model.ShopDetailTab
import com.peto.ramap.ui.shop.model.ShopOverviewUiState
import com.peto.ramap.ui.shop.overview.OverviewContent
import com.peto.ramap.ui.shop.overview.model.ExternalLinkActions
import com.peto.ramap.ui.shop.overview.model.HeaderActions
import com.peto.ramap.ui.shop.overview.model.OverviewActions
import com.peto.ramap.ui.shop.overview.model.OverviewUiState
import com.peto.ramap.ui.shop.review.ReviewsContent
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.hide_shop_confirm_action
import ramap.shared.generated.resources.hide_shop_confirm_description
import ramap.shared.generated.resources.hide_shop_confirm_dismiss
import ramap.shared.generated.resources.hide_shop_confirm_title
import ramap.shared.generated.resources.ic_map_outline
import ramap.shared.generated.resources.ranking_show_shop_on_map

@Composable
fun ShopDetailContent(
    uiState: ShopDetailContentUiState,
    config: ShopDetailSheetConfig,
    actions: ShopDetailActions,
    modifier: Modifier = Modifier,
) {
    val state = uiState.sheetState
    val selectedShop =
        when (state) {
            ShopOverviewUiState.Closed -> null
            is ShopOverviewUiState.Loading -> state.shop
            is ShopOverviewUiState.Content -> state.overview.shop
            is ShopOverviewUiState.Error -> state.shop
        }
    var hideConfirmShop by remember(uiState.shopId) { mutableStateOf<RamenShop?>(null) }
    var showReportDialog by remember(uiState.shopId) { mutableStateOf(false) }
    var selectedTab by remember(uiState.shopId, config.showReviewsOnOpen) {
        mutableStateOf(if (config.showReviewsOnOpen) ShopDetailTab.REVIEW else ShopDetailTab.MENU)
    }

    if (config.visible) {
        val listState = key(uiState.shopId) { rememberLazyListState() }
        val mapAction =
            if (uiState.displayState == ShopDetailDisplayState.Content && state is ShopOverviewUiState.Content && actions.externalLinkActions.onShowOnMap != null) {
                { actions.externalLinkActions.onShowOnMap.invoke(state.overview.shop.id) }
            } else {
                null
            }
        ReviewsContent(
            shopName = selectedShop?.name.orEmpty(),
            uiState = uiState.reviewState,
            actions = actions.reviewActions,
        ) { reviewItems, loadMoreState ->
            ShopDetailBottomSheet(
                shopId = uiState.shopId,
                listState = listState,
                onDismissRequest = actions.onDismissRequest,
                isBackEnabled = config.isBackEnabled,
                maxHeight = config.maxHeight,
                isNavigationBarPadded = config.isNavigationBarPadded,
                loadMoreState = loadMoreState,
                loadMoreEnabled = uiState.displayState == ShopDetailDisplayState.Content && selectedTab == ShopDetailTab.REVIEW,
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
            ) {
                when (uiState.displayState) {
                    ShopDetailDisplayState.Initial ->
                        item(key = "shop_detail_initial_loading", contentType = "status") {
                            ShopDetailSkeleton(selectedTab = selectedTab)
                        }

                    ShopDetailDisplayState.OverviewLoading ->
                        item(key = "shop_detail_overview_loading", contentType = "status") {
                            RamenLoadingIndicator(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 240.dp),
                            )
                        }

                    ShopDetailDisplayState.Content -> {
                        val overview = requireNotNull((state as? ShopOverviewUiState.Content)?.overview)
                        val shop = overview.shop
                        item(key = "shop_detail_overview", contentType = "overview") {
                            val overviewUiState =
                                OverviewUiState(
                                    overview = overview,
                                    headerState = uiState.headerState,
                                )
                            val overviewActions =
                                OverviewActions(
                                    headerActions =
                                        HeaderActions(
                                            onDismissRequest = actions.onDismissRequest,
                                            onBookmarkClick = { actions.shopActions.onBookmarkToggled(shop) },
                                            onNotificationClick = { actions.shopActions.onShopNotificationToggled(shop) },
                                            onHiddenClick = {
                                                if (uiState.isLoggedIn && !uiState.headerState.isHidden) {
                                                    hideConfirmShop = shop
                                                } else {
                                                    actions.shopActions.onHiddenToggled(shop)
                                                }
                                            },
                                            onReportClick = { showReportDialog = true },
                                            onShareClick = { actions.shopActions.onShopShareClick(shop) },
                                            onEventClick = actions.shopActions.onEventClick,
                                            onOperatingNoticeClick = actions.shopActions.onOperatingNoticeClick,
                                        ),
                                    externalLinkActions =
                                        ExternalLinkActions(
                                            onMapLinkClick = { provider -> actions.shopActions.onShopMapLinkClick(shop, provider) },
                                            onWaitingClick = actions.externalLinkActions.onWaitingClick,
                                            onExternalLinkClick = actions.externalLinkActions.onExternalLinkClick,
                                            onAppleMapsClick = actions.shopActions.onAppleMapsClick,
                                        ),
                                )
                            OverviewContent(
                                uiState = overviewUiState,
                                actions = overviewActions,
                            )
                        }
                        if (uiState.overviewHasLoadFailure) {
                            item(key = "shop_detail_overview_refresh_error", contentType = "status") { RegionLoadError(actions.onRetry) }
                        }
                        shopDetailSections(
                            scope = this,
                            uiState = uiState,
                            actions = actions,
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            reviewItems = reviewItems,
                        )
                    }

                    ShopDetailDisplayState.OverviewError ->
                        item(key = "shop_detail_overview_error", contentType = "status") {
                            RegionLoadError(onRetry = actions.onRetry)
                        }
                }
            }
        }
    }

    CommonDialog(
        visible = hideConfirmShop != null,
        confirmText = stringResource(Res.string.hide_shop_confirm_action),
        dismissText = stringResource(Res.string.hide_shop_confirm_dismiss),
        onDismissRequest = { hideConfirmShop = null },
        content = {
            AppText(
                text = stringResource(Res.string.hide_shop_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = stringResource(Res.string.hide_shop_confirm_description),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = {
            hideConfirmShop?.let(actions.shopActions.onHiddenToggled)
            hideConfirmShop = null
        },
        onDismiss = { hideConfirmShop = null },
    )

    selectedShop?.let { shop ->
        ReportDialog(
            shop = shop,
            visible = showReportDialog,
            onDismissRequest = { showReportDialog = false },
            onSubmit = { wrongFields, description ->
                showReportDialog = false
                actions.shopActions.onReportSubmit(wrongFields, description)
            },
        )
    }
}
