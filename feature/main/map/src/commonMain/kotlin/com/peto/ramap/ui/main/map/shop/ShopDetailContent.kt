package com.peto.ramap.ui.main.map.shop

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.button.AppExtendedFloatingActionButton
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.map.dialog.ReportDialog
import com.peto.ramap.ui.main.map.shop.model.RamenShopOverviewActions
import com.peto.ramap.ui.main.map.shop.model.RamenShopOverviewUiState
import com.peto.ramap.ui.main.map.shop.model.RamenShopUiModel
import com.peto.ramap.ui.main.map.shop.model.ShopDetailSheetUiState
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewExternalLinkActions
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewHeaderActions
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewHeaderUiState
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewReviewActions
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewReviewPagingUiState
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewUserContext
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
    state: ShopDetailSheetUiState,
    isBackEnabled: Boolean,
    maxHeight: Dp,
    isNavigationBarPadded: Boolean = false,
    visible: Boolean = true,
    showRequestedLoadingInSheet: Boolean = false,
    showReviewsOnOpen: Boolean = false,
    waitingSystem: WaitingSystemUiModel? = null,
    isBookmarked: Boolean = false,
    isNotificationEnabled: Boolean = false,
    showNotificationActions: Boolean = true,
    isHidden: Boolean = false,
    isLoggedIn: Boolean = false,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
    onBookmarkToggled: (RamenShop) -> Unit,
    onShopNotificationToggled: (RamenShop) -> Unit,
    onHiddenToggled: (RamenShop) -> Unit,
    onShopShareClick: (RamenShop) -> Unit,
    onShopMapLinkClick: (RamenShop, String) -> Unit,
    onEventClick: (ShopEvent) -> Unit,
    onOperatingNoticeClick: (OperatingNotice) -> Unit = {},
    onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    onShowOnMap: ((String) -> Unit)? = null,
    onWaitingClick: (String) -> Unit = {},
    onExternalLinkClick: (String) -> Unit = {},
    isAppleMapsAvailable: Boolean = false,
    onAppleMapsClick: (RamenShop) -> Unit = {},
    onReviewsClick: (String) -> Unit,
    currentUserId: String? = null,
    currentProfileIsPublic: Boolean? = null,
    actingReviewId: String? = null,
    revealedBlockedReviews: Map<String, Review> = emptyMap(),
    revealingBlockedReviewId: String? = null,
    onViewBlockedReview: ((Review) -> Unit)? = null,
    onUnblockBlockedUser: ((Review) -> Unit)? = null,
    onOpenProfile: (String) -> Unit = {},
    onReviewLike: (Review) -> Unit = {},
    onReviewEdit: (Review) -> Unit = {},
    onReviewDelete: (Review) -> Unit = {},
    onReviewReport: (Review) -> Unit = {},
    hasMoreReviews: Boolean = false,
    isLoadingMoreReviews: Boolean = false,
    isRetryingReviews: Boolean = false,
    onLoadMoreReviews: () -> Unit = {},
) {
    val selectedShop =
        when (state) {
            ShopDetailSheetUiState.Closed -> null
            is ShopDetailSheetUiState.Loading -> state.shop
            is ShopDetailSheetUiState.Content -> state.detail.shop
            is ShopDetailSheetUiState.Error -> state.shop
        }
    var hideConfirmShop by remember { mutableStateOf<RamenShop?>(null) }
    var showReportDialog by remember(selectedShop?.id) { mutableStateOf(false) }
    var deleteReview by remember(selectedShop?.id) { mutableStateOf<Review?>(null) }
    val shouldShowMainSheet =
        selectedShop != null ||
            (showRequestedLoadingInSheet && state is ShopDetailSheetUiState.Loading)
    val mainSheetShopId =
        when (state) {
            ShopDetailSheetUiState.Closed -> null
            is ShopDetailSheetUiState.Loading -> state.shopId
            is ShopDetailSheetUiState.Content -> state.detail.shop.id
            is ShopDetailSheetUiState.Error -> state.shopId
        }

    if (visible && shouldShowMainSheet && state !is ShopDetailSheetUiState.Error) {
        val mapAction =
            if (state is ShopDetailSheetUiState.Content && onShowOnMap != null) {
                { onShowOnMap(state.detail.shop.id) }
            } else {
                null
            }
        ShopDetailBottomSheet(
            shopId = requireNotNull(mainSheetShopId),
            onDismissRequest = onDismissRequest,
            isBackEnabled = isBackEnabled,
            maxHeight = maxHeight,
            isNavigationBarPadded = isNavigationBarPadded,
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
                is ShopDetailSheetUiState.Loading ->
                    RamenLoadingIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp),
                    )

                is ShopDetailSheetUiState.Content -> {
                    val shop = state.detail.shop
                    val overviewUiState =
                        RamenShopOverviewUiState(
                            detail = state.detail,
                            headerState =
                                ShopOverviewHeaderUiState(
                                    waitingSystem = waitingSystem,
                                    isBookmarked = isBookmarked,
                                    isNotificationEnabled = isNotificationEnabled,
                                    showNotificationActions = showNotificationActions,
                                    isHidden = isHidden,
                                    isAppleMapsAvailable = isAppleMapsAvailable,
                                ),
                            reviewPagingState =
                                ShopOverviewReviewPagingUiState(
                                    isRetryingReviews = isRetryingReviews,
                                    showReviewsOnOpen = showReviewsOnOpen,
                                    hasMoreReviews = hasMoreReviews,
                                    isLoadingMoreReviews = isLoadingMoreReviews,
                                ),
                            userContext =
                                ShopOverviewUserContext(
                                    currentUserId = currentUserId,
                                    currentProfileIsPublic = currentProfileIsPublic,
                                    actingReviewId = actingReviewId,
                                    revealedBlockedReviews = revealedBlockedReviews,
                                    revealingBlockedReviewId = revealingBlockedReviewId,
                                ),
                        )
                    val overviewActions =
                        RamenShopOverviewActions(
                            headerActions =
                                ShopOverviewHeaderActions(
                                    onDismissRequest = onDismissRequest,
                                    onBookmarkClick = { onBookmarkToggled(shop) },
                                    onNotificationClick = { onShopNotificationToggled(shop) },
                                    onHiddenClick = {
                                        if (isLoggedIn && !isHidden) {
                                            hideConfirmShop = shop
                                        } else {
                                            onHiddenToggled(shop)
                                        }
                                    },
                                    onReportClick = { showReportDialog = true },
                                    onShareClick = { onShopShareClick(shop) },
                                    onEventClick = onEventClick,
                                    onOperatingNoticeClick = onOperatingNoticeClick,
                                ),
                            externalLinkActions =
                                ShopOverviewExternalLinkActions(
                                    onMapLinkClick = { provider -> onShopMapLinkClick(shop, provider) },
                                    onWaitingClick = onWaitingClick,
                                    onExternalLinkClick = onExternalLinkClick,
                                    onAppleMapsClick = onAppleMapsClick,
                                ),
                            reviewActions =
                                ShopOverviewReviewActions(
                                    onOpenProfile = onOpenProfile,
                                    onWriteReviewClick = { onReviewsClick(shop.id) },
                                    onReviewRetry = onRetry,
                                    onViewBlockedReview = onViewBlockedReview,
                                    onUnblockBlockedUser = onUnblockBlockedUser,
                                    onReviewLike = onReviewLike,
                                    onReviewEdit = onReviewEdit,
                                    onReviewDelete = { deleteReview = it },
                                    onReviewReport = onReviewReport,
                                    onLoadMoreReviews = onLoadMoreReviews,
                                ),
                        )
                    RamenShopOverview(
                        uiState = overviewUiState,
                        actions = overviewActions,
                        reviewScrollState = scrollState,
                        bottomContentPadding = if (mapAction != null) 50.dp else 0.dp,
                    )
                }

                ShopDetailSheetUiState.Closed,
                is ShopDetailSheetUiState.Error,
                -> Unit
            }
        }
    }

    if (visible && state is ShopDetailSheetUiState.Error) {
        CommonBottomSheet(
            visible = visible,
            onDismissRequest = onDismissRequest,
            isBackEnabled = isBackEnabled,
            config =
                CommonBottomSheetConfig(
                    maxHeight = maxHeight,
                    isDraggable = true,
                    isStatusBarPadded = true,
                    isNavigationBarPadded = isNavigationBarPadded,
                ),
        ) { _ ->
            LoadErrorContent(
                image = Res.drawable.laduck_error_crying,
                title = stringResource(Res.string.map_shop_detail_error_title),
                description = stringResource(Res.string.map_shop_detail_error_description),
                onRetry = onRetry,
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
            hideConfirmShop?.let(onHiddenToggled)
            hideConfirmShop = null
        },
        onDismiss = { hideConfirmShop = null },
    )

    selectedShop?.let { shop ->
        ReportDialog(
            shopUiModel =
                RamenShopUiModel(
                    shop = shop,
                    waitingVisible = waitingSystem != null,
                ),
            visible = showReportDialog,
            onDismissRequest = { showReportDialog = false },
            onSubmit = { wrongFields, description ->
                showReportDialog = false
                onReportSubmit(wrongFields, description)
            },
        )
    }
    CommonDialog(
        visible = deleteReview != null,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = deleteReview?.id != actingReviewId,
        confirmIsLoading = deleteReview?.id == actingReviewId,
        dismissOnBackPress = deleteReview?.id != actingReviewId,
        dismissOnClickOutside = deleteReview?.id != actingReviewId,
        onDismissRequest = { if (deleteReview?.id != actingReviewId) deleteReview = null },
        onDismiss = { deleteReview = null },
        onConfirm = {
            deleteReview?.let(onReviewDelete)
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
