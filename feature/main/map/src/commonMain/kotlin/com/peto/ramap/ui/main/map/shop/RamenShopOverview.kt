package com.peto.ramap.ui.main.map.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewCardHeader
import com.peto.ramap.designsystem.review.ReviewWriteCard
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.shop.model.RamenShopOverviewActions
import com.peto.ramap.ui.main.map.shop.model.RamenShopOverviewUiState
import com.peto.ramap.ui.main.map.shop.model.ShopDetailTab
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewExternalLinkActions
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewHeaderActions
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewHeaderUiState
import com.peto.ramap.ui.main.map.shop.model.ShopOverviewReviewActions
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.map_shop_detail_error_description
import ramap.shared.generated.resources.review_empty_illustration
import ramap.shared.generated.resources.shop_review_empty_description
import ramap.shared.generated.resources.shop_review_empty_title

@Composable
fun RamenShopOverview(
    uiState: RamenShopOverviewUiState,
    actions: RamenShopOverviewActions,
    modifier: Modifier = Modifier,
    dragAreaModifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
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

    val currentHasMoreReviews = rememberUpdatedState(reviewPagingState.hasMoreReviews)
    val currentIsLoadingMoreReviews = rememberUpdatedState(reviewPagingState.isLoadingMoreReviews)
    val currentOnLoadMoreReviews = rememberUpdatedState(reviewActions.onLoadMoreReviews)

    LaunchedEffect(listState) {
        var canRequestAfterLeavingBottom = true
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val isNearBottom = totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
            Pair(
                isNearBottom,
                Pair(
                    currentHasMoreReviews.value,
                    !currentIsLoadingMoreReviews.value,
                ),
            )
        }.collect { (isNearBottom, canLoadMore) ->
            val (hasMore, isNotLoading) = canLoadMore
            if (!isNearBottom) {
                canRequestAfterLeavingBottom = true
            } else if (canRequestAfterLeavingBottom && hasMore && isNotLoading) {
                canRequestAfterLeavingBottom = false
                currentOnLoadMoreReviews.value()
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item {
            ShopDetailSheetHandle(dragModifier = dragAreaModifier)
        }
        item {
            ShopHeaderSection(
                shop = shop,
                likeCount = uiState.detail.likeCount,
                isBookmarked = headerState.isBookmarked,
                isNotificationEnabled = headerState.isNotificationEnabled,
                showNotificationActions = headerState.showNotificationActions,
                isHidden = headerState.isHidden,
                event = uiState.detail.event,
                dragAreaModifier = Modifier,
                onDismissRequest = headerActions.onDismissRequest,
                onBookmarkClick = headerActions.onBookmarkClick,
                onNotificationClick = headerActions.onNotificationClick,
                onHiddenClick = headerActions.onHiddenClick,
                onReportClick = headerActions.onReportClick,
                onShareClick = headerActions.onShareClick,
                onEventClick = headerActions.onEventClick,
            )
        }

        if (shop.businessHoursDetails != null || uiState.detail.operatingNotice != null) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    BusinessHoursCard(
                        shop = shop,
                        operatingNotice = uiState.detail.operatingNotice,
                        operatingNotices = uiState.detail.operatingNotices,
                        onOperatingNoticeClick = headerActions.onOperatingNoticeClick,
                    )
                }
            }
        }

        item {
            ShopExternalLinksRow(
                shop = shop,
                waitingSystem = headerState.waitingSystem,
                isAppleMapsAvailable = headerState.isAppleMapsAvailable,
                onMapLinkClick = externalLinkActions.onMapLinkClick,
                onWaitingClick = externalLinkActions.onWaitingClick,
                onExternalLinkClick = externalLinkActions.onExternalLinkClick,
                onAppleMapsClick = externalLinkActions.onAppleMapsClick,
            )
        }

        stickyHeader {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(CommonColor.White)
                        .padding(horizontal = 20.dp, vertical = 6.dp),
            ) {
                ShopDetailTabRow(
                    menuCount = uiState.detail.menuItemCount,
                    reviewCount = uiState.detail.reviewCount,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            }
        }

        when (selectedTab) {
            ShopDetailTab.MENU -> {
                item {
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
            }
            ShopDetailTab.REVIEW -> {
                if (uiState.detail.hasReviewLoadFailure && reviewPagingState.isRetryingReviews) {
                    item {
                        RamenLoadingIndicator(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                        )
                    }
                } else if (uiState.detail.hasReviewLoadFailure) {
                    item {
                        LoadErrorContent(
                            image = Res.drawable.laduck_error_crying,
                            title = stringResource(Res.string.data_load_failure_message),
                            description = stringResource(Res.string.map_shop_detail_error_description),
                            onRetry = reviewActions.onReviewRetry,
                            compact = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (uiState.detail.reviews.isEmpty() && !uiState.detail.hasReviewLoadFailure) {
                    item {
                        LoadErrorContent(
                            image = Res.drawable.review_empty_illustration,
                            title = stringResource(Res.string.shop_review_empty_title),
                            description =
                                stringResource(
                                    Res.string.shop_review_empty_description,
                                    shop.name,
                                ),
                            compact = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        ReviewWriteCard(
                            onClick = reviewActions.onWriteReviewClick,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    item {
                        ReviewWriteCard(
                            onClick = reviewActions.onWriteReviewClick,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                        )
                    }
                    items(uiState.detail.reviews, key = { it.id }) { review ->
                        val revealedReview = userContext.revealedBlockedReviews[review.id]
                        val displayedReview = revealedReview ?: review
                        val cardActions =
                            ReviewCardActions(
                                onOpenProfile = reviewActions.onOpenProfile,
                                onLike =
                                    if (
                                        !review.isBlocked &&
                                        review.author.userId != userContext.currentUserId &&
                                        review.isPublic &&
                                        review.moderationStatus == ReviewModerationStatus.PUBLISHED
                                    ) {
                                        { reviewActions.onReviewLike(review) }
                                    } else {
                                        null
                                    },
                                onReport =
                                    if (
                                        (!review.isBlocked || revealedReview != null) &&
                                        displayedReview.author.userId.isNotBlank() &&
                                        displayedReview.author.userId != userContext.currentUserId
                                    ) {
                                        { reviewActions.onReviewReport(displayedReview) }
                                    } else {
                                        null
                                    },
                                onEdit =
                                    if (userContext.currentUserId != null && review.author.userId == userContext.currentUserId) {
                                        { reviewActions.onReviewEdit(review) }
                                    } else {
                                        null
                                    },
                                onDelete =
                                    if (userContext.currentUserId != null && review.author.userId == userContext.currentUserId) {
                                        { reviewActions.onReviewDelete(review) }
                                    } else {
                                        null
                                    },
                                onViewBlockedReview =
                                    if (review.isBlocked && reviewActions.onViewBlockedReview != null) {
                                        { reviewActions.onViewBlockedReview.invoke(review) }
                                    } else {
                                        null
                                    },
                                onUnblockBlockedUser =
                                    if (review.isBlocked && reviewActions.onUnblockBlockedUser != null) {
                                        { reviewActions.onUnblockBlockedUser.invoke(review) }
                                    } else {
                                        null
                                    },
                            )
                        ReviewCard(
                            review = displayedReview,
                            actions = cardActions,
                            header = {
                                ReviewCardHeader(
                                    review = displayedReview,
                                    actions = cardActions,
                                    actionsEnabled = userContext.actingReviewId == null,
                                )
                            },
                            showBlockedContent = review.isBlocked && revealedReview != null,
                            isBlockedReviewLoading = userContext.revealingBlockedReviewId == review.id,
                            currentUserId = userContext.currentUserId,
                            currentProfileIsPublic = userContext.currentProfileIsPublic,
                            isLikeLoading = userContext.actingReviewId == review.id,
                            actionsEnabled = userContext.actingReviewId == null,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (reviewPagingState.hasMoreReviews && reviewPagingState.isLoadingMoreReviews) {
                        item {
                            RamenLoadingIndicator(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                            )
                        }
                    }
                }
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
                    detail = ShopDetail(shop = shop, likeCount = 0L, waitingSystem = null, event = null, operatingNotice = null),
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
