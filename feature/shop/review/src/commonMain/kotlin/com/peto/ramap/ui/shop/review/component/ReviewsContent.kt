package com.peto.ramap.ui.shop.review.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.review.ReviewWriteCard
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState
import com.peto.ramap.ui.shop.review.model.ReviewActions
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_shop_detail_error_description
import ramap.shared.generated.resources.review_empty_illustration
import ramap.shared.generated.resources.review_load_error_illustration
import ramap.shared.generated.resources.shop_review_empty_description
import ramap.shared.generated.resources.shop_review_empty_title
import ramap.shared.generated.resources.shop_review_load_failure_message

internal fun reviewsContent(
    scope: LazyListScope,
    shopName: String,
    uiState: ReviewsUiState,
    actions: ReviewActions,
    onDeleteReview: (Review) -> Unit,
) {
    if (uiState.isLoading && !uiState.hasLoaded) {
        scope.item(key = "shop_review_initial_loading", contentType = "review_status") {
            ShopReviewSkeleton()
        }
        return
    }
    if (uiState.hasReviewLoadFailure) {
        scope.item(key = "shop_review_error", contentType = "review_status") {
            if (uiState.isLoading) {
                RamenLoadingIndicator(modifier = Modifier.fillMaxWidth().height(120.dp))
            } else {
                LoadErrorContent(
                    image = Res.drawable.review_load_error_illustration,
                    title = stringResource(Res.string.shop_review_load_failure_message),
                    description = stringResource(Res.string.map_shop_detail_error_description),
                    onRetry = actions.onRetry,
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    } else if (uiState.reviews.isEmpty()) {
        scope.item(key = "shop_review_empty", contentType = "review_status") {
            LoadErrorContent(
                image = Res.drawable.review_empty_illustration,
                title = stringResource(Res.string.shop_review_empty_title),
                description = stringResource(Res.string.shop_review_empty_description, shopName),
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    scope.item(key = "shop_review_write", contentType = "review_write") {
        ReviewWriteCard(
            onClick = actions.onWriteReviewClick,
            modifier =
                Modifier.fillMaxWidth().padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (uiState.hasReviewLoadFailure || uiState.reviews.isEmpty()) 20.dp else 0.dp,
                ),
        )
    }
    scope.items(
        items = uiState.reviews,
        key = { reviewItemKey(it.id) },
        contentType = { "review" },
    ) { review ->
        ShopReviewItem(
            review = review,
            uiState = uiState,
            actions = actions,
            onDeleteReview = onDeleteReview,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
    if (uiState.hasMoreReviews && uiState.isLoadingMoreReviews) {
        scope.item(key = "shop_review_page_loading", contentType = "review_status") {
            RamenLoadingIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(120.dp),
            )
        }
    }
}

internal fun reviewItemKey(reviewId: String): String = "shop_review:$reviewId"

@Preview(showBackground = true)
@Composable
private fun ReviewsContentPreview() {
    RamapTheme {
        LazyColumn {
            reviewsContent(
                scope = this,
                shopName = "멘야 하나비",
                uiState =
                    ReviewsUiState(
                        hasLoaded = true,
                        page =
                            ShopReviewsPage(
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
                                totalCount = 2,
                            ),
                    ),
                actions =
                    ReviewActions(
                        onReviewLike = {},
                        onRetry = {},
                        onLoadMoreReviews = {},
                        onWriteReviewClick = {},
                        onOpenProfile = {},
                        onReviewEdit = {},
                        onReviewReport = {},
                        onViewBlockedReview = {},
                        onUnblockBlockedUser = {},
                        onReviewDelete = {},
                    ),
                onDeleteReview = {},
            )
        }
    }
}
