package com.peto.ramap.ui.main.feed.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewCardHeader
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.preview.ReviewPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.feed.contract.FeedIntent
import com.peto.ramap.ui.main.feed.contract.FeedUiState

@Composable
internal fun FeedReviewItem(
    row: ProfileReview,
    state: FeedUiState,
    onIntent: (FeedIntent) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenShop: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    reviewCommentsContent: (@Composable (String) -> Unit)? = null,
    onOpenReviewDetail: ((String) -> Unit)? = null,
) {
    val original = row.review
    val displayed = state.revealed[original.id] ?: original
    val isOwner = original.author.userId == state.currentUserId
    val actions =
        ReviewCardActions(
            onOpenProfile = onOpenProfile.takeUnless { original.isBlocked },
            onLike = { onIntent(FeedIntent.ToggleLike(original)) }.takeIf { !original.isBlocked && !isOwner },
            onReport = { onIntent(FeedIntent.RequestReport(displayed)) }.takeIf { !isOwner && (!original.isBlocked || original.id in state.revealed) },
            onEdit = { onEditReview(original.shopId, original.id) }.takeIf { isOwner },
            onDelete = { onIntent(FeedIntent.RequestDelete(original)) }.takeIf { isOwner },
            onViewBlockedReview = { onIntent(FeedIntent.Reveal(original)) }.takeIf { original.isBlocked },
            onUnblockBlockedUser = { onIntent(FeedIntent.RequestUnblock(original)) }.takeIf { original.isBlocked },
            onOpenReviewDetail = onOpenReviewDetail?.let { open -> { open(original.id) } },
        )
    ReviewCard(
        review = displayed,
        actions = actions,
        currentUserId = state.currentUserId,
        actionsEnabled = state.actingId == null && !state.unblocking,
        isLikeLoading = state.actingId == original.id,
        showBlockedContent = original.id in state.revealed,
        isBlockedReviewLoading = state.revealingId == original.id,
        commentsContent = reviewCommentsContent,
        header = {
            ReviewCardHeader(
                review = displayed,
                actions = actions,
                actionsEnabled = state.actingId == null,
                shop = row.shop,
                onOpenShop = { onOpenShop(row.shop.id) },
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun FeedReviewItemPreview() {
    val review = ReviewPreviewParameterProvider().reviewPreviewSamples.first()
    val shop = RamenShopPreviewParameterProvider().ramenShopPreviewSamples.first().copy(id = review.shopId)
    val row = ProfileReview(review = review, shop = shop)
    RamapTheme {
        FeedReviewItem(
            row = row,
            state =
                FeedUiState(
                    hasLoaded = true,
                    sessionResolved = true,
                    currentUserId = "my-user-id",
                ),
            onIntent = {},
            onOpenProfile = {},
            onOpenShop = {},
            onEditReview = { _, _ -> },
        )
    }
}
