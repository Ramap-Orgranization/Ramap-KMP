package com.peto.ramap.ui.shop.review.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewCardHeader
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState
import com.peto.ramap.ui.shop.review.model.ReviewActions

@Composable
internal fun ShopReviewItem(
    review: Review,
    uiState: ReviewsUiState,
    actions: ReviewActions,
    onDeleteReview: (Review) -> Unit,
    modifier: Modifier = Modifier,
) {
    val revealedReview = uiState.revealedBlockedReviews[review.id]
    val displayedReview = revealedReview ?: review
    val reviewActions =
        ReviewCardActions(
            onOpenProfile = actions.onOpenProfile,
            onLike =
                if (
                    !review.isBlocked &&
                    review.author.userId != uiState.currentUserId &&
                    review.isPublic &&
                    review.moderationStatus == ReviewModerationStatus.PUBLISHED
                ) {
                    { actions.onReviewLike(review) }
                } else {
                    null
                },
            onReport =
                if (
                    (!review.isBlocked || revealedReview != null) &&
                    displayedReview.author.userId.isNotBlank() &&
                    displayedReview.author.userId != uiState.currentUserId
                ) {
                    { actions.onReviewReport(displayedReview) }
                } else {
                    null
                },
            onEdit =
                if (uiState.currentUserId != null && review.author.userId == uiState.currentUserId) {
                    { actions.onReviewEdit(review) }
                } else {
                    null
                },
            onDelete =
                if (uiState.currentUserId != null && review.author.userId == uiState.currentUserId) {
                    { onDeleteReview(review) }
                } else {
                    null
                },
            onViewBlockedReview =
                if (review.isBlocked && actions.onViewBlockedReview != null) {
                    { actions.onViewBlockedReview(review) }
                } else {
                    null
                },
            onUnblockBlockedUser =
                if (review.isBlocked && actions.onUnblockBlockedUser != null) {
                    { actions.onUnblockBlockedUser(review) }
                } else {
                    null
                },
        )
    ReviewCard(
        review = displayedReview,
        actions = reviewActions,
        modifier = modifier,
        header = {
            ReviewCardHeader(
                review = displayedReview,
                actions = reviewActions,
                actionsEnabled = uiState.actingReviewId == null,
            )
        },
        showBlockedContent = review.isBlocked && revealedReview != null,
        isBlockedReviewLoading = uiState.revealingBlockedReviewId == review.id,
        currentUserId = uiState.currentUserId,
        currentProfileIsPublic = uiState.currentProfileIsPublic,
        isLikeLoading = uiState.actingReviewId == review.id,
        actionsEnabled = uiState.actingReviewId == null,
    )
}
