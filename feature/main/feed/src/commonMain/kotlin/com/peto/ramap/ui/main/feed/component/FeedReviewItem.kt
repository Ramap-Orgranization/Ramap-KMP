package com.peto.ramap.ui.main.feed.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.RamenShopCompactSummary
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
        )
    ReviewCard(
        review = displayed,
        actions = actions,
        currentUserId = state.currentUserId,
        actionsEnabled = state.actingId == null && !state.unblocking,
        isLikeLoading = state.actingId == original.id,
        showBlockedContent = original.id in state.revealed,
        isBlockedReviewLoading = state.revealingId == original.id,
        header = {
            Column {
                ReviewCardHeader(
                    review = displayed,
                    actions = actions,
                    actionsEnabled = state.actingId == null,
                )
                RamenShopCompactSummary(
                    shop = row.shop,
                    onClick = { onOpenShop(row.shop.id) },
                    modifier =
                        Modifier
                            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 7.dp)
                            .clip(RoundedCornerShape(12.dp)),
                )
            }
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
