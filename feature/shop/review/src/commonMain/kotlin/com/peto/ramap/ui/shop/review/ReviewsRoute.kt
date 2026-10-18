package com.peto.ramap.ui.shop.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.shop.review.component.dialog.ShopBlockedReviewUnblockDialog
import com.peto.ramap.ui.shop.review.component.dialog.ShopReviewReportDialog
import com.peto.ramap.ui.shop.review.contract.ReviewsIntent
import com.peto.ramap.ui.shop.review.contract.ReviewsSideEffect
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState
import com.peto.ramap.ui.shop.review.model.ReviewActions
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewsRoute(
    shopId: String,
    isLoggedIn: Boolean,
    onReviewNavigate: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    onLoginGuideRequested: () -> Unit,
    onLoginRequired: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: ReviewsViewModel = koinViewModel(),
    content: @Composable (ReviewsUiState, ReviewActions) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoginGuideRequested by rememberUpdatedState(onLoginGuideRequested)
    val currentOnLoginRequired by rememberUpdatedState(onLoginRequired)
    val currentToastManager by rememberUpdatedState(toastManager)

    LaunchedEffect(viewModel, shopId) {
        viewModel.dispatch(ReviewsIntent.OnShopSelected(shopId))
    }

    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            ReviewsSideEffect.ShowLoginGuide -> {
                currentOnLoginRequired()
                currentOnLoginGuideRequested()
            }
            is ReviewsSideEffect.ShowToast -> currentToastManager.show(ToastData(effect.message, effect.type))
        }
    }

    content(
        uiState,
        ReviewActions(
            onWriteReviewClick = {
                if (isLoggedIn) onReviewNavigate(shopId) else onLoginGuideRequested()
            },
            onRetry = { viewModel.dispatch(ReviewsIntent.OnRetry) },
            onViewBlockedReview = { viewModel.dispatch(ReviewsIntent.OnBlockedReviewViewRequested(it)) },
            onUnblockBlockedUser = { viewModel.dispatch(ReviewsIntent.OnBlockedReviewUnblockRequested(it)) },
            onOpenProfile = onOpenProfile,
            onReviewLike = { viewModel.dispatch(ReviewsIntent.OnReviewLikeToggled(it)) },
            onReviewEdit = { onEditReview(it.shopId, it.id) },
            onReviewDelete = { viewModel.dispatch(ReviewsIntent.OnReviewDeleted(it)) },
            onReviewReport = { viewModel.dispatch(ReviewsIntent.OnReviewReportRequested(it)) },
            onLoadMoreReviews = { viewModel.dispatch(ReviewsIntent.OnLoadMore) },
        ),
    )

    ShopReviewReportDialog(state = uiState, onIntent = viewModel::dispatch)
    ShopBlockedReviewUnblockDialog(state = uiState, onIntent = viewModel::dispatch)
}
