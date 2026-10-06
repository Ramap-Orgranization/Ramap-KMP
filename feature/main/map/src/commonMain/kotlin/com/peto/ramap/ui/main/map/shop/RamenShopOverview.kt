package com.peto.ramap.ui.main.map.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewCardHeader
import com.peto.ramap.designsystem.review.ReviewWriteCard
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.shop.model.ShopDetailTab
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
    shop: RamenShop,
    likeCount: Long,
    waitingSystem: WaitingSystemUiModel?,
    isBookmarked: Boolean,
    isNotificationEnabled: Boolean,
    showNotificationActions: Boolean,
    isHidden: Boolean,
    isAppleMapsAvailable: Boolean,
    event: ShopEvent?,
    operatingNotice: OperatingNotice?,
    operatingNotices: List<OperatingNotice>,
    menuSections: List<MenuSection>,
    menuUpdatedAt: String?,
    reviews: List<Review>,
    reviewCount: Int,
    hasReviewLoadFailure: Boolean = false,
    isRetryingReviews: Boolean = false,
    menuItemCount: Int,
    showReviewsOnOpen: Boolean = false,
    modifier: Modifier = Modifier,
    dragAreaModifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onBookmarkClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onHiddenClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareClick: () -> Unit,
    onMapLinkClick: (String) -> Unit,
    onWaitingClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onAppleMapsClick: (RamenShop) -> Unit,
    onEventClick: (ShopEvent) -> Unit,
    onOperatingNoticeClick: (OperatingNotice) -> Unit,
    onOpenProfile: (String) -> Unit,
    onWriteReviewClick: () -> Unit,
    onReviewRetry: () -> Unit = {},
    currentUserId: String? = null,
    currentProfileIsPublic: Boolean? = null,
    actingReviewId: String? = null,
    revealedBlockedReviews: Map<String, Review> = emptyMap(),
    revealingBlockedReviewId: String? = null,
    onViewBlockedReview: ((Review) -> Unit)? = null,
    onUnblockBlockedUser: ((Review) -> Unit)? = null,
    onReviewLike: (Review) -> Unit = {},
    onReviewEdit: (Review) -> Unit = {},
    onReviewDelete: (Review) -> Unit = {},
    onReviewReport: (Review) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    hasMoreReviews: Boolean = false,
    isLoadingMoreReviews: Boolean = false,
    onLoadMoreReviews: () -> Unit = {},
    menuFooter: @Composable () -> Unit = {},
) {
    var selectedTab by remember(shop.id, showReviewsOnOpen) {
        mutableStateOf(if (showReviewsOnOpen) ShopDetailTab.REVIEW else ShopDetailTab.MENU)
    }

    val currentHasMoreReviews = rememberUpdatedState(hasMoreReviews)
    val currentIsLoadingMoreReviews = rememberUpdatedState(isLoadingMoreReviews)
    val currentOnLoadMoreReviews = rememberUpdatedState(onLoadMoreReviews)

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
    ) {
        item {
            ShopDetailSheetHandle(dragModifier = dragAreaModifier)
        }
        item {
            ShopHeaderSection(
                shop = shop,
                likeCount = likeCount,
                isBookmarked = isBookmarked,
                isNotificationEnabled = isNotificationEnabled,
                showNotificationActions = showNotificationActions,
                isHidden = isHidden,
                event = event,
                dragAreaModifier = Modifier,
                onDismissRequest = onDismissRequest,
                onBookmarkClick = onBookmarkClick,
                onNotificationClick = onNotificationClick,
                onHiddenClick = onHiddenClick,
                onReportClick = onReportClick,
                onShareClick = onShareClick,
                onEventClick = onEventClick,
            )
        }

        if (shop.businessHoursDetails != null || operatingNotice != null) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    BusinessHoursCard(
                        shop = shop,
                        operatingNotice = operatingNotice,
                        operatingNotices = operatingNotices,
                        onOperatingNoticeClick = onOperatingNoticeClick,
                    )
                }
            }
        }

        item {
            ShopExternalLinksRow(
                shop = shop,
                waitingSystem = waitingSystem,
                isAppleMapsAvailable = isAppleMapsAvailable,
                onMapLinkClick = onMapLinkClick,
                onWaitingClick = onWaitingClick,
                onExternalLinkClick = onExternalLinkClick,
                onAppleMapsClick = onAppleMapsClick,
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
                    menuCount = menuItemCount,
                    reviewCount = reviewCount,
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
                            sections = menuSections,
                            updatedAt = menuUpdatedAt,
                            onMenuSourceClick = onExternalLinkClick,
                        )
                    }
                }
                item {
                    menuFooter()
                }
            }
            ShopDetailTab.REVIEW -> {
                if (hasReviewLoadFailure && isRetryingReviews) {
                    item {
                        RamenLoadingIndicator(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                        )
                    }
                } else if (hasReviewLoadFailure) {
                    item {
                        LoadErrorContent(
                            image = Res.drawable.laduck_error_crying,
                            title = stringResource(Res.string.data_load_failure_message),
                            description = stringResource(Res.string.map_shop_detail_error_description),
                            onRetry = onReviewRetry,
                            compact = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (reviews.isEmpty() && !hasReviewLoadFailure) {
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
                            onClick = onWriteReviewClick,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    item {
                        ReviewWriteCard(
                            onClick = onWriteReviewClick,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                        )
                    }
                    items(reviews, key = { it.id }) { review ->
                        val revealedReview = revealedBlockedReviews[review.id]
                        val displayedReview = revealedReview ?: review
                        val reviewActions =
                            ReviewCardActions(
                                onOpenProfile = onOpenProfile,
                                onLike =
                                    if (
                                        !review.isBlocked &&
                                        review.author.userId != currentUserId &&
                                        review.isPublic &&
                                        review.moderationStatus == ReviewModerationStatus.PUBLISHED
                                    ) {
                                        { onReviewLike(review) }
                                    } else {
                                        null
                                    },
                                onReport =
                                    if (
                                        (!review.isBlocked || revealedReview != null) &&
                                        displayedReview.author.userId.isNotBlank() &&
                                        displayedReview.author.userId != currentUserId
                                    ) {
                                        { onReviewReport(displayedReview) }
                                    } else {
                                        null
                                    },
                                onEdit =
                                    if (currentUserId != null && review.author.userId == currentUserId) {
                                        { onReviewEdit(review) }
                                    } else {
                                        null
                                    },
                                onDelete =
                                    if (currentUserId != null && review.author.userId == currentUserId) {
                                        { onReviewDelete(review) }
                                    } else {
                                        null
                                    },
                                onViewBlockedReview =
                                    if (review.isBlocked && onViewBlockedReview != null) {
                                        { onViewBlockedReview(review) }
                                    } else {
                                        null
                                    },
                                onUnblockBlockedUser =
                                    if (review.isBlocked && onUnblockBlockedUser != null) {
                                        { onUnblockBlockedUser(review) }
                                    } else {
                                        null
                                    },
                            )
                        ReviewCard(
                            review = displayedReview,
                            actions = reviewActions,
                            header = {
                                ReviewCardHeader(
                                    review = displayedReview,
                                    actions = reviewActions,
                                    actionsEnabled = actingReviewId == null,
                                )
                            },
                            showBlockedContent = review.isBlocked && revealedReview != null,
                            isBlockedReviewLoading = revealingBlockedReviewId == review.id,
                            currentUserId = currentUserId,
                            currentProfileIsPublic = currentProfileIsPublic,
                            isLikeLoading = actingReviewId == review.id,
                            actionsEnabled = actingReviewId == null,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (hasMoreReviews && isLoadingMoreReviews) {
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
            shop = shop,
            likeCount = 0L,
            waitingSystem = null,
            isBookmarked = false,
            isNotificationEnabled = false,
            showNotificationActions = true,
            isHidden = false,
            isAppleMapsAvailable = true,
            event = null,
            operatingNotice = null,
            operatingNotices = emptyList(),
            menuSections = emptyList(),
            menuUpdatedAt = null,
            reviews = emptyList(),
            reviewCount = 0,
            menuItemCount = 0,
            onDismissRequest = {},
            dragAreaModifier = Modifier,
            onBookmarkClick = {},
            onNotificationClick = {},
            onHiddenClick = {},
            onReportClick = {},
            onShareClick = {},
            onMapLinkClick = {},
            onWaitingClick = {},
            onExternalLinkClick = {},
            onAppleMapsClick = {},
            onEventClick = {},
            onOperatingNoticeClick = {},
            onOpenProfile = {},
            onWriteReviewClick = {},
        )
    }
}
