package com.peto.ramap.ui.main.map.shop

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.review.ReviewsContent
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.shop.model.RamenShopOverviewActions
import com.peto.ramap.ui.main.map.shop.model.RamenShopOverviewUiState
import com.peto.ramap.ui.main.map.shop.model.ShopDetailTab
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewExternalLinkActions
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewHeaderActions
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewHeaderUiState
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewReviewActions

@Composable
fun RamenShopOverview(
    uiState: RamenShopOverviewUiState,
    actions: RamenShopOverviewActions,
    modifier: Modifier = Modifier,
    dragAreaModifier: Modifier = Modifier,
    reviewScrollState: ScrollState = rememberScrollState(),
    bottomContentPadding: Dp = 0.dp,
) {
    val shop = uiState.detail.shop
    val headerState = uiState.headerState
    val reviewPagingState = uiState.reviewPagingState
    val userContext = uiState.userContext
    val headerActions = actions.headerActions
    val externalLinkActions = actions.externalLinkActions
    val reviewActions = actions.reviewActions

    var selectedTab by remember(shop.id, reviewPagingState.showReviewsOnOpen) {
        mutableStateOf(if (reviewPagingState.showReviewsOnOpen) ShopDetailTab.REVIEW else ShopDetailTab.MENU)
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp + bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShopHeaderSection(
            shop = shop,
            likeCount = uiState.detail.likeCount,
            isBookmarked = headerState.isBookmarked,
            isNotificationEnabled = headerState.isNotificationEnabled,
            showNotificationActions = headerState.showNotificationActions,
            isHidden = headerState.isHidden,
            event = uiState.detail.event,
            dragAreaModifier = dragAreaModifier,
            onDismissRequest = headerActions.onDismissRequest,
            onBookmarkClick = headerActions.onBookmarkClick,
            onNotificationClick = headerActions.onNotificationClick,
            onHiddenClick = headerActions.onHiddenClick,
            onReportClick = headerActions.onReportClick,
            onShareClick = headerActions.onShareClick,
            onEventClick = headerActions.onEventClick,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (shop.businessHoursDetails != null || uiState.detail.operatingNotice != null) {
                BusinessHoursCard(
                    shop = shop,
                    operatingNotice = uiState.detail.operatingNotice,
                    operatingNotices = uiState.detail.operatingNotices,
                    onOperatingNoticeClick = headerActions.onOperatingNoticeClick,
                )
            }
        }

        ShopExternalLinksRow(
            shop = shop,
            waitingSystem = headerState.waitingSystem,
            isAppleMapsAvailable = headerState.isAppleMapsAvailable,
            onMapLinkClick = externalLinkActions.onMapLinkClick,
            onWaitingClick = externalLinkActions.onWaitingClick,
            onExternalLinkClick = externalLinkActions.onExternalLinkClick,
            onAppleMapsClick = externalLinkActions.onAppleMapsClick,
        )

        ShopDetailTabRow(
            menuCount = uiState.detail.menuItemCount,
            reviewCount = uiState.detail.reviewCount,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        when (selectedTab) {
            ShopDetailTab.MENU -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    ShopMenuContent(
                        sections = uiState.detail.menuSections,
                        updatedAt = uiState.detail.menuUpdatedAt,
                        onMenuSourceClick = externalLinkActions.onExternalLinkClick,
                    )
                }
            }
            ShopDetailTab.REVIEW -> {
                ReviewsContent(
                    shopName = shop.name,
                    reviews = uiState.detail.reviews,
                    hasReviewLoadFailure = uiState.detail.hasReviewLoadFailure,
                    isRetryingReviews = reviewPagingState.isRetryingReviews,
                    scrollState = reviewScrollState,
                    onOpenProfile = reviewActions.onOpenProfile,
                    onWriteReviewClick = reviewActions.onWriteReviewClick,
                    onRetry = reviewActions.onReviewRetry,
                    currentUserId = userContext.currentUserId,
                    currentProfileIsPublic = userContext.currentProfileIsPublic,
                    actingReviewId = userContext.actingReviewId,
                    revealedBlockedReviews = userContext.revealedBlockedReviews,
                    revealingBlockedReviewId = userContext.revealingBlockedReviewId,
                    onViewBlockedReview = reviewActions.onViewBlockedReview,
                    onUnblockBlockedUser = reviewActions.onUnblockBlockedUser,
                    onLike = reviewActions.onReviewLike,
                    onEdit = reviewActions.onReviewEdit,
                    onDelete = reviewActions.onReviewDelete,
                    onReport = reviewActions.onReviewReport,
                    hasMoreReviews = reviewPagingState.hasMoreReviews,
                    isLoadingMoreReviews = reviewPagingState.isLoadingMoreReviews,
                    onLoadMoreReviews = reviewActions.onLoadMoreReviews,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RamenShopOverviewPreview(
    @PreviewParameter(RamenShopPreviewParameterProvider::class) shop: RamenShop,
) {
    RamapTheme {
        RamenShopOverview(
            uiState =
                RamenShopOverviewUiState(
                    detail =
                        ShopDetail(
                            shop = shop,
                            likeCount = 0L,
                            waitingSystem = null,
                            event = null,
                            operatingNotice = null,
                        ),
                    headerState = ShopOverviewHeaderUiState(isAppleMapsAvailable = true),
                ),
            actions =
                RamenShopOverviewActions(
                    headerActions =
                        ShopOverviewHeaderActions(
                            onDismissRequest = {},
                            onBookmarkClick = {},
                            onNotificationClick = {},
                            onHiddenClick = {},
                            onReportClick = {},
                            onShareClick = {},
                            onEventClick = {},
                        ),
                    externalLinkActions =
                        ShopOverviewExternalLinkActions(
                            onMapLinkClick = {},
                            onWaitingClick = {},
                            onExternalLinkClick = {},
                            onAppleMapsClick = {},
                        ),
                    reviewActions =
                        ShopOverviewReviewActions(
                            onOpenProfile = {},
                            onWriteReviewClick = {},
                        ),
                ),
            dragAreaModifier = Modifier,
        )
    }
}
