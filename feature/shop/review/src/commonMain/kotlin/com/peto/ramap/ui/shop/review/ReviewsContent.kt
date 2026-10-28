package com.peto.ramap.ui.shop.review

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.peto.ramap.designsystem.dialog.ReviewDeleteDialog
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.paging.InfiniteScrollState
import com.peto.ramap.ui.shop.review.component.reviewItemKey
import com.peto.ramap.ui.shop.review.component.reviewsContent
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState
import com.peto.ramap.ui.shop.review.model.ReviewActions

@Composable
fun ReviewsContent(
    shopName: String,
    uiState: ReviewsUiState,
    actions: ReviewActions,
    content: @Composable (reviewItems: LazyListScope.() -> Unit, loadMoreState: InfiniteScrollState) -> Unit,
) {
    var deleteReview by remember(uiState.shopId) { mutableStateOf<Review?>(null) }

    val itemKeys = remember(uiState.reviews) { uiState.reviews.map { reviewItemKey(it.id) } }
    val loadMoreState =
        InfiniteScrollState(
            itemCount = uiState.reviews.size,
            hasMore = uiState.hasMoreReviews && !uiState.hasReviewLoadFailure,
            isLoading = uiState.isLoadingMoreReviews,
            isRefreshing = uiState.isLoading,
            itemKeys = itemKeys,
            requestKey = uiState.currentUserId,
        )
    val reviewItems: LazyListScope.() -> Unit = {
        reviewsContent(
            scope = this,
            shopName = shopName,
            uiState = uiState,
            actions = actions,
            onDeleteReview = { deleteReview = it },
        )
    }
    content(reviewItems, loadMoreState)

    ReviewDeleteDialog(
        visible = deleteReview != null,
        isDeleting = deleteReview?.id == uiState.actingReviewId,
        onDismissRequest = { deleteReview = null },
        onConfirm = {
            deleteReview?.let(actions.onReviewDelete)
            deleteReview = null
        },
    )
}
