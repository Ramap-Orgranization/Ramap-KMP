package com.peto.ramap.designsystem.review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.preview.ReviewPreviewParameterProvider
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier,
    actions: ReviewCardActions = ReviewCardActions(),
    currentUserId: String? = null,
    currentProfileIsPublic: Boolean? = null,
    isLikeLoading: Boolean = false,
    actionsEnabled: Boolean = true,
    showBlockedContent: Boolean = false,
    isBlockedReviewLoading: Boolean = false,
    commentsContent: (@Composable (String) -> Unit)? = null,
    showFullBody: Boolean = false,
    showWrittenTime: Boolean = true,
    photoViewerEnabled: Boolean = false,
    header: @Composable () -> Unit,
) {
    if (review.isBlocked && !showBlockedContent) {
        BlockedReviewCard(
            modifier = modifier,
            onViewReview = actions.onViewBlockedReview,
            onUnblockUser = actions.onUnblockBlockedUser,
            isLoading = isBlockedReviewLoading,
        )
        return
    }

    val canExpandComments = !review.isBlocked && commentsContent != null && (review.commentCount ?: 0) > 0
    var commentsExpanded by rememberSaveable(review.id, canExpandComments) { mutableStateOf(false) }
    val showComments = canExpandComments && commentsExpanded
    val onOpenReviewDetail = actions.onOpenReviewDetail.takeIf { actionsEnabled && !review.isBlocked }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = CommonColor.White,
        ) {
            Column {
                header()

                Spacer(modifier = Modifier.height(5.dp))

                if (review.imageUrls.isNotEmpty()) {
                    ReviewCardImages(
                        imageUrls = review.imageUrls,
                        photoViewerEnabled = photoViewerEnabled,
                        onPhotoClick = onOpenReviewDetail,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                ReviewCardContent(
                    review = review,
                    currentUserId = currentUserId,
                    currentProfileIsPublic = currentProfileIsPublic,
                    showFullBody = showFullBody,
                    onBodyClick = onOpenReviewDetail,
                )

                Spacer(modifier = Modifier.height(10.dp))
                ReviewCardFooter(
                    review = review,
                    onLike = actions.onLike,
                    isLikeLoading = isLikeLoading,
                    actionsEnabled = actionsEnabled,
                    commentsExpanded = showComments,
                    canExpandComments = canExpandComments,
                    showWrittenTime = showWrittenTime,
                    onCommentsToggle = commentsContent?.let { { commentsExpanded = !commentsExpanded } },
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = GrayColor.C100)
            }
        }
        if (showComments && commentsContent != null) {
            Surface(color = CommonColor.White) {
                Column {
                    commentsContent(review.id)
                    HorizontalDivider(color = GrayColor.C100)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedReviewCardPreview(
    @PreviewParameter(ReviewPreviewParameterProvider::class) review: Review,
) {
    RamapTheme {
        Column {
            ReviewCard(
                review = review.copy(isBlocked = true),
                actions =
                    ReviewCardActions(
                        onViewBlockedReview = {},
                        onUnblockBlockedUser = {},
                    ),
                header = {
                    ReviewCardHeader(
                        review = review.copy(isBlocked = true),
                        actions =
                            ReviewCardActions(
                                onViewBlockedReview = {},
                                onUnblockBlockedUser = {},
                            ),
                        actionsEnabled = true,
                    )
                },
            )
            ReviewCard(
                review = review,
                header = {
                    ReviewCardHeader(
                        review = review,
                        actions = ReviewCardActions(),
                        actionsEnabled = true,
                    )
                },
            )
        }
    }
}
