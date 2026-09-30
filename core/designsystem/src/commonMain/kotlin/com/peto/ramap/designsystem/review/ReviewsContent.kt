package com.peto.ramap.designsystem.review

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.map_shop_detail_error_description
import ramap.shared.generated.resources.review_empty_illustration
import ramap.shared.generated.resources.shop_review_empty_description
import ramap.shared.generated.resources.shop_review_empty_title

@Composable
internal fun ReviewsContent(
    shopName: String,
    reviews: List<Review>,
    hasReviewLoadFailure: Boolean = false,
    isRetryingReviews: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    onOpenProfile: (String) -> Unit,
    onWriteReviewClick: () -> Unit,
    onRetry: (() -> Unit)? = null,
    currentUserId: String? = null,
    currentProfileIsPublic: Boolean? = null,
    actingReviewId: String? = null,
    revealedBlockedReviews: Map<String, Review> = emptyMap(),
    revealingBlockedReviewId: String? = null,
    onViewBlockedReview: ((Review) -> Unit)? = null,
    onUnblockBlockedUser: ((Review) -> Unit)? = null,
    onLike: (Review) -> Unit = {},
    onEdit: (Review) -> Unit = {},
    onDelete: (Review) -> Unit = {},
    onReport: (Review) -> Unit = {},
    hasMoreReviews: Boolean = false,
    isLoadingMoreReviews: Boolean = false,
    onLoadMoreReviews: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentHasMoreReviews = rememberUpdatedState(hasMoreReviews)
    val currentIsLoadingMoreReviews = rememberUpdatedState(isLoadingMoreReviews)
    val currentOnLoadMoreReviews = rememberUpdatedState(onLoadMoreReviews)
    val prefetchThresholdPx = with(LocalDensity.current) { 320.dp.roundToPx() }
    LaunchedEffect(scrollState, prefetchThresholdPx) {
        var canRequestAfterLeavingBottom = true
        snapshotFlow {
            val remainingScrollDistance = scrollState.maxValue - scrollState.value
            val isNearBottom = scrollState.maxValue > 0 && remainingScrollDistance <= prefetchThresholdPx
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

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (hasReviewLoadFailure && isRetryingReviews) {
            RamenLoadingIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
            )
        } else if (hasReviewLoadFailure) {
            LoadErrorContent(
                image = Res.drawable.laduck_error_crying,
                title = stringResource(Res.string.data_load_failure_message),
                description = stringResource(Res.string.map_shop_detail_error_description),
                onRetry = onRetry,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (reviews.isEmpty() && !hasReviewLoadFailure) {
            LoadErrorContent(
                image = Res.drawable.review_empty_illustration,
                title = stringResource(Res.string.shop_review_empty_title),
                description =
                    stringResource(
                        Res.string.shop_review_empty_description,
                        shopName,
                    ),
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ReviewWriteCard(
                onClick = onWriteReviewClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )
        } else {
            ReviewWriteCard(
                onClick = onWriteReviewClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )
            reviews.forEach { review ->
                key(review.id) {
                    val revealedReview = revealedBlockedReviews[review.id]
                    val displayedReview = revealedReview ?: review
                    ReviewCard(
                        review = displayedReview,
                        showBlockedContent = review.isBlocked && revealedReview != null,
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
                        isBlockedReviewLoading = revealingBlockedReviewId == review.id,
                        currentUserId = currentUserId,
                        currentProfileIsPublic = currentProfileIsPublic,
                        onOpenProfile = onOpenProfile,
                        onLike =
                            if (
                                !review.isBlocked &&
                                review.author.userId != currentUserId &&
                                review.isPublic &&
                                review.moderationStatus == ReviewModerationStatus.PUBLISHED
                            ) {
                                { onLike(review) }
                            } else {
                                null
                            },
                        isLikeLoading = actingReviewId == review.id,
                        actionsEnabled = actingReviewId == null,
                        onEdit =
                            if (currentUserId != null && review.author.userId == currentUserId) {
                                { onEdit(review) }
                            } else {
                                null
                            },
                        onDelete =
                            if (currentUserId != null && review.author.userId == currentUserId) {
                                { onDelete(review) }
                            } else {
                                null
                            },
                        onReport =
                            if (!review.isBlocked && review.author.userId.isNotBlank() && review.author.userId != currentUserId) {
                                { onReport(review) }
                            } else {
                                null
                            },
                    )
                }
            }
            if (hasMoreReviews && isLoadingMoreReviews) {
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

@Preview(showBackground = true)
@Composable
private fun ReviewsContentPreview() {
    RamapTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            ReviewsContent(
                shopName = "멘야 하나비",
                reviews = emptyList(),
                onOpenProfile = {},
                onWriteReviewClick = {},
            )
            ReviewsContent(
                shopName = "멘야 하나비",
                reviews =
                    listOf(
                        Review(
                            id = "preview-review-1",
                            shopId = "preview-shop",
                            body = "국물이 진하고 면발의 식감이 아주 좋습니다. 또 방문하고 싶네요!",
                            createdAt = "2026-02-15T12:00:00Z",
                            imageUrls = emptyList(),
                            author = ReviewAuthor("preview-author-1", "면발수집가"),
                        ),
                        Review(
                            id = "preview-review-2",
                            shopId = "preview-shop",
                            body = "자가제면이라 면발이 탱탱해요.",
                            createdAt = "2026-02-14T18:30:00Z",
                            imageUrls = emptyList(),
                            author = ReviewAuthor("preview-author-2", "라멘러버"),
                        ),
                    ),
                onOpenProfile = {},
                onWriteReviewClick = {},
            )
        }
    }
}
