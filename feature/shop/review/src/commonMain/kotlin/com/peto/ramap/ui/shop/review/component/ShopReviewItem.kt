package com.peto.ramap.ui.shop.review.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewCardHeader
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
    val canChangeOwnReview = !uiState.isUsingGuestReviews && uiState.currentUserId != null && review.allowsDeletionBy(uiState.currentUserId)
    val reviewActions =
        ReviewCardActions(
            onOpenProfile = actions.onOpenProfile,
            onOpenReviewDetail = actions.onOpenReviewDetail?.let { open -> { open(review.id) } },
            onLike =
                if (
                    !review.isBlocked &&
                    !uiState.isUsingGuestReviews &&
                    review.allowsLikeFrom(uiState.currentUserId)
                ) {
                    { actions.onReviewLike(review) }
                } else {
                    null
                },
            onReport =
                if (
                    !uiState.isUsingGuestReviews &&
                    (!review.isBlocked || revealedReview != null) &&
                    (displayedReview.author.userId.isNotBlank() || displayedReview.author.isWithdrawn) &&
                    displayedReview.author.userId != uiState.currentUserId
                ) {
                    { actions.onReviewReport(displayedReview) }
                } else {
                    null
                },
            onEdit =
                if (canChangeOwnReview) {
                    { actions.onReviewEdit(review) }
                } else {
                    null
                },
            onDelete =
                if (canChangeOwnReview) {
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
        commentsContent = actions.reviewCommentsContent,
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
