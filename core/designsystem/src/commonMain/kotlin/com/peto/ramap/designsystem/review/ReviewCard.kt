package com.peto.ramap.designsystem.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.extension.noRippleClickable
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

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    actions.onReviewClick?.let { onReviewClick ->
                        Modifier.noRippleClickable(
                            enabled = actionsEnabled,
                            onClick = onReviewClick,
                        )
                    } ?: Modifier,
                ),
        shape = RectangleShape,
        color = CommonColor.White,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            header()

            ReviewCardImages(
                imageUrls = review.imageUrls,
                photoViewerEnabled = actions.onReviewClick == null,
            )

            ReviewCardContent(
                review = review,
                currentUserId = currentUserId,
                currentProfileIsPublic = currentProfileIsPublic,
            )

            ReviewCardFooter(
                review = review,
                onLike = actions.onLike,
                isLikeLoading = isLikeLoading,
                actionsEnabled = actionsEnabled,
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = GrayColor.C100)
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
