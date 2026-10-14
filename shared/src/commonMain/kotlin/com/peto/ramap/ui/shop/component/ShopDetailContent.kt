package com.peto.ramap.ui.shop.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.button.AppExtendedFloatingActionButton
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.review.ReviewsContent
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.shop.component.OverviewTabRow
import com.peto.ramap.ui.shop.component.dialog.ReportDialog
import com.peto.ramap.ui.shop.menu.ShopMenuContent
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailSheetConfig
import com.peto.ramap.ui.shop.model.ShopDetailTab
import com.peto.ramap.ui.shop.model.ShopOverviewUiState
import com.peto.ramap.ui.shop.overview.OverviewContent
import com.peto.ramap.ui.shop.overview.model.ExternalLinkActions
import com.peto.ramap.ui.shop.overview.model.HeaderActions
import com.peto.ramap.ui.shop.overview.model.OverviewActions
import com.peto.ramap.ui.shop.overview.model.OverviewUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.hide_shop_confirm_action
import ramap.shared.generated.resources.hide_shop_confirm_description
import ramap.shared.generated.resources.hide_shop_confirm_dismiss
import ramap.shared.generated.resources.hide_shop_confirm_title
import ramap.shared.generated.resources.ic_map_outline
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.map_shop_detail_error_description
import ramap.shared.generated.resources.map_shop_detail_error_title
import ramap.shared.generated.resources.ranking_show_shop_on_map
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_delete_confirm
import ramap.shared.generated.resources.review_delete_confirm_title
import ramap.shared.generated.resources.shop_review_cancel

@Composable
fun ShopDetailContent(
    uiState: ShopDetailContentUiState,
    config: ShopDetailSheetConfig,
    actions: ShopDetailActions,
    modifier: Modifier = Modifier,
) {
    val state = uiState.sheetState
    if (state is ShopOverviewUiState.Closed) return
    val selectedShop =
        when (state) {
            ShopOverviewUiState.Closed -> null
            is ShopOverviewUiState.Loading -> state.shop
            is ShopOverviewUiState.Content -> state.overview.shop
            is ShopOverviewUiState.Error -> state.shop
        }
    var hideConfirmShop by remember { mutableStateOf<RamenShop?>(null) }
    var showReportDialog by remember(selectedShop?.id) { mutableStateOf(false) }
    var deleteReview by remember(selectedShop?.id) { mutableStateOf<Review?>(null) }
    var selectedTab by remember(selectedShop?.id, config.showReviewsOnOpen) {
        mutableStateOf(if (config.showReviewsOnOpen) ShopDetailTab.REVIEW else ShopDetailTab.MENU)
    }

    val hasOtherContent = !uiState.isShopUnavailable && (uiState.menuState != null || uiState.reviewState != null)
    val shouldShowMainSheet =
        selectedShop != null || hasOtherContent || (config.showRequestedLoadingInSheet && state is ShopOverviewUiState.Loading)
    val mainSheetShopId =
        when (state) {
            ShopOverviewUiState.Closed -> null
            is ShopOverviewUiState.Loading -> state.shopId
            is ShopOverviewUiState.Content -> state.overview.shop.id
            is ShopOverviewUiState.Error -> state.shopId
        }

    if (config.visible && shouldShowMainSheet && (state !is ShopOverviewUiState.Error || hasOtherContent)) {
        val mapAction =
            if (state is ShopOverviewUiState.Content && actions.externalLinkActions.onShowOnMap != null) {
                { actions.externalLinkActions.onShowOnMap.invoke(state.overview.shop.id) }
            } else {
                null
            }
        ShopDetailBottomSheet(
            shopId = requireNotNull(mainSheetShopId),
            onDismissRequest = actions.onDismissRequest,
            isBackEnabled = config.isBackEnabled,
            maxHeight = config.maxHeight,
            isNavigationBarPadded = config.isNavigationBarPadded,
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
        ) { scrollState ->
            when (state) {
                is ShopOverviewUiState.Loading ->
                    RamenLoadingIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp),
                    )

                is ShopOverviewUiState.Content -> {
                    val shop = state.overview.shop
                    val overviewUiState =
                        OverviewUiState(
                            overview = state.overview,
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

                is ShopOverviewUiState.Error -> RegionLoadError(onRetry = actions.onRetry)
                ShopOverviewUiState.Closed -> Unit
            }
            if (uiState.overviewHasLoadFailure && state is ShopOverviewUiState.Content) RegionLoadError(actions.onRetry)
            if (state is ShopOverviewUiState.Content || uiState.reviewState != null) {
                ShopDetailSections(
                    uiState = uiState,
                    actions = actions,
                    shopId = requireNotNull(mainSheetShopId),
                    shopName = selectedShop?.name.orEmpty(),
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    scrollState = scrollState,
                    onDeleteReview = { deleteReview = it },
                )
            }
        }
    }

    if (config.visible && state is ShopOverviewUiState.Error && !hasOtherContent) {
        CommonBottomSheet(
            visible = config.visible,
            onDismissRequest = actions.onDismissRequest,
            isBackEnabled = config.isBackEnabled,
            config =
                CommonBottomSheetConfig(
                    maxHeight = config.maxHeight,
                    isDraggable = true,
                    isStatusBarPadded = true,
                    isNavigationBarPadded = config.isNavigationBarPadded,
                ),
        ) { _ ->
            LoadErrorContent(
                image = Res.drawable.laduck_error_crying,
                title = stringResource(Res.string.map_shop_detail_error_title),
                description = stringResource(Res.string.map_shop_detail_error_description),
                onRetry = actions.onRetry,
                modifier = Modifier.fillMaxWidth(),
                compact = true,
            )
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

    CommonDialog(
        visible = deleteReview != null,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = deleteReview?.id != uiState.userContext.actingReviewId,
        confirmIsLoading = deleteReview?.id == uiState.userContext.actingReviewId,
        dismissOnBackPress = deleteReview?.id != uiState.userContext.actingReviewId,
        dismissOnClickOutside = deleteReview?.id != uiState.userContext.actingReviewId,
        onDismissRequest = { if (deleteReview?.id != uiState.userContext.actingReviewId) deleteReview = null },
        onDismiss = { deleteReview = null },
        onConfirm = {
            deleteReview?.let(actions.reviewActions.onReviewDelete)
            deleteReview = null
        },
    ) {
        AppText(
            text = stringResource(Res.string.review_delete_confirm_title),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
        AppText(
            text = stringResource(Res.string.review_delete_confirm),
            modifier = Modifier.padding(top = 8.dp),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RegionLoadError(onRetry: () -> Unit) {
    LoadErrorContent(
        image = Res.drawable.laduck_error_crying,
        title = stringResource(Res.string.map_shop_detail_error_title),
        description = stringResource(Res.string.map_shop_detail_error_description),
        onRetry = onRetry,
        modifier = Modifier.fillMaxWidth(),
        compact = true,
    )
}

@Composable
private fun ShopDetailSections(
    uiState: ShopDetailContentUiState,
    actions: ShopDetailActions,
    shopId: String,
    shopName: String,
    selectedTab: ShopDetailTab,
    onTabSelected: (ShopDetailTab) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    onDeleteReview: (Review) -> Unit,
) {
    OverviewTabRow(
        menuCount = uiState.menuState?.menu?.itemCount ?: 0,
        reviewCount = uiState.reviewState?.reviewCount ?: 0,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
    when (selectedTab) {
        ShopDetailTab.MENU -> {
            ShopMenuContent(
                uiState =
                    uiState.menuState ?: com.peto.ramap.ui.shop.menu.contract
                        .ShopMenuUiState(),
                onRetry = actions.onMenuRetry,
                onMenuSourceClick = actions.externalLinkActions.onExternalLinkClick,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        ShopDetailTab.REVIEW -> {
            if (uiState.reviewState?.isLoading == true && !uiState.reviewState.hasLoaded) {
                RamenLoadingIndicator(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp))
            } else {
                ReviewsContent(
                    shopName = shopName,
                    reviews = uiState.reviewState?.reviews.orEmpty(),
                    hasReviewLoadFailure = uiState.reviewState?.hasReviewLoadFailure == true,
                    isRetryingReviews = uiState.reviewPagingState.isRetryingReviews,
                    scrollState = scrollState,
                    onOpenProfile = actions.reviewActions.onOpenProfile,
                    onWriteReviewClick = { actions.reviewActions.onReviewsClick(shopId) },
                    onRetry = actions.onReviewsRetry,
                    currentUserId = uiState.userContext.currentUserId,
                    currentProfileIsPublic = uiState.userContext.currentProfileIsPublic,
                    actingReviewId = uiState.userContext.actingReviewId,
                    revealedBlockedReviews = uiState.userContext.revealedBlockedReviews,
                    revealingBlockedReviewId = uiState.userContext.revealingBlockedReviewId,
                    onViewBlockedReview = actions.reviewActions.onViewBlockedReview,
                    onUnblockBlockedUser = actions.reviewActions.onUnblockBlockedUser,
                    onLike = actions.reviewActions.onReviewLike,
                    onEdit = actions.reviewActions.onReviewEdit,
                    onDelete = onDeleteReview,
                    onReport = actions.reviewActions.onReviewReport,
                    hasMoreReviews = uiState.reviewPagingState.hasMoreReviews,
                    isLoadingMoreReviews = uiState.reviewPagingState.isLoadingMoreReviews,
                    onLoadMoreReviews = actions.reviewActions.onLoadMoreReviews,
                )
            }
        }
    }
}
