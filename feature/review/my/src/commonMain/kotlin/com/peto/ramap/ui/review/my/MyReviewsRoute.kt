package com.peto.ramap.ui.review.my

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewCardHeader
import com.peto.ramap.designsystem.review.ReviewPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.preview.MyReviewsPagePreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.paging.ObserveLoadMoreNearListEnd
import com.peto.ramap.ui.review.my.component.MyReviewCardSkeleton
import com.peto.ramap.ui.review.my.component.MyReviewEmpty
import com.peto.ramap.ui.review.my.component.MyReviewFilterHeader
import com.peto.ramap.ui.review.my.component.MyReviewsFilterSkeleton
import com.peto.ramap.ui.review.my.contract.MyReviewsIntent
import com.peto.ramap.ui.review.my.contract.MyReviewsLoadKey
import com.peto.ramap.ui.review.my.contract.MyReviewsSideEffect
import com.peto.ramap.ui.review.my.contract.MyReviewsUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.my_reviews
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_delete_confirm
import ramap.shared.generated.resources.review_delete_confirm_title
import ramap.shared.generated.resources.review_load_failed
import ramap.shared.generated.resources.review_retry
import ramap.shared.generated.resources.shop_review_cancel

@Composable
fun MyReviewsRoute(
    onBack: () -> Unit,
    onShowShop: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: MyReviewsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is MyReviewsSideEffect.ShowToast -> toastManager.show(effect.data)
        }
    }
    MyReviewsContent(
        state = state,
        onBack = onBack,
        onShowShop = onShowShop,
        onEditReview = onEditReview,
        onIntent = viewModel::dispatch,
    )
}

@Composable
fun MyReviewsContent(
    state: MyReviewsUiState,
    onBack: () -> Unit,
    onShowShop: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    onIntent: (MyReviewsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteTargetReview by remember { mutableStateOf<MyReview?>(null) }
    val listState = rememberLazyListState()
    ObserveLoadMoreNearListEnd(
        listState = listState,
        itemThreshold = PREFETCH_ITEM_THRESHOLD,
        hasMore = state.hasMore,
        isLoading = state.loading,
        onLoadMore = { onIntent(MyReviewsIntent.LoadMore) },
    )

    ReviewPage(
        title = stringResource(Res.string.my_reviews),
        onBack = onBack,
    ) {
        LazyColumn(
            state = listState,
            modifier =
                modifier
                    .fillMaxWidth()
                    .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.showsInitialLoading) {
                item {
                    MyReviewsFilterSkeleton(
                        modifier = Modifier.padding(start = 15.dp),
                    )
                }
                items(3) { index ->
                    MyReviewCardSkeleton(showPhotos = index == 0)
                }
            } else {
                item {
                    MyReviewFilterHeader(
                        state = state,
                        onSelectFilter = { onIntent(MyReviewsIntent.SelectFilter(it)) },
                        modifier = Modifier.padding(start = 15.dp),
                    )
                }

                if (state.failed) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppText(
                                text = stringResource(Res.string.review_load_failed),
                                style = AppTextStyle.B3,
                                color = SystemColor.Warning,
                            )
                            AppButton(
                                text = stringResource(Res.string.review_retry),
                                onClick = { onIntent(MyReviewsIntent.Retry) },
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 0.dp,
                            )
                        }
                    }
                }

                if (state.showsEmptyReviews) {
                    item {
                        MyReviewEmpty(
                            filter = state.filter,
                            modifier = Modifier.fillParentMaxHeight(0.8f),
                        )
                    }
                }
                items(
                    items = state.reviews,
                    key = { it.id },
                ) { review ->
                    val reviewActions =
                        ReviewCardActions(
                            onShopClick = { onShowShop(review.shopId) },
                            onEdit = { onEditReview(review.shopId, review.id) },
                            onDelete = { deleteTargetReview = review },
                        )
                    ReviewCard(
                        review = review.toReview(),
                        actions = reviewActions,
                        header = {
                            ReviewCardHeader(
                                review = review.toReview(),
                                actions = reviewActions,
                                actionsEnabled = state.actingReviewId == null,
                            )
                        },
                        actionsEnabled = state.actingReviewId == null,
                    )
                }
                if (state.showsAppendLoading) {
                    item {
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
    }
    CommonDialog(
        visible = deleteTargetReview != null,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = deleteTargetReview?.id != state.actingReviewId,
        confirmIsLoading = deleteTargetReview?.id == state.actingReviewId,
        dismissOnBackPress = deleteTargetReview?.id != state.actingReviewId,
        dismissOnClickOutside = deleteTargetReview?.id != state.actingReviewId,
        onDismissRequest = { if (deleteTargetReview?.id != state.actingReviewId) deleteTargetReview = null },
        onDismiss = { deleteTargetReview = null },
        onConfirm = {
            deleteTargetReview?.let { onIntent(MyReviewsIntent.DeleteReview(it.id)) }
            deleteTargetReview = null
        },
    ) {
        AppText(
            text = stringResource(Res.string.review_delete_confirm_title),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
        )
        AppText(
            text = stringResource(Res.string.review_delete_confirm),
            modifier = Modifier.padding(top = 8.dp),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
        )
    }
}

private const val PREFETCH_ITEM_THRESHOLD = 3

@Preview(showBackground = true)
@Composable
private fun MyReviewsContentPreview(
    @PreviewParameter(MyReviewsPagePreviewParameterProvider::class) page: MyReviewsPage,
) {
    RamapTheme {
        MyReviewsContent(
            state =
                MyReviewsUiState(
                    filter = MyReviewVisibility.ALL,
                    reviews = page.reviews,
                    totalCount = page.totalCount,
                    publicCount = page.publicCount,
                    privateCount = page.privateCount,
                    profileIsPublic = page.profileIsPublic,
                    loaded = true,
                ),
            onBack = {},
            onShowShop = {},
            onEditReview = { _, _ -> },
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyReviewsContentLoadingPreview() {
    RamapTheme {
        MyReviewsContent(
            state =
                MyReviewsUiState(
                    filter = MyReviewVisibility.ALL,
                    reviews = emptyList(),
                    loadState = LoadState.loading(MyReviewsLoadKey.Page),
                ),
            onBack = {},
            onShowShop = {},
            onEditReview = { _, _ -> },
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyReviewsContentEmptyPreview() {
    RamapTheme {
        MyReviewsContent(
            state =
                MyReviewsUiState(
                    filter = MyReviewVisibility.ALL,
                    reviews = emptyList(),
                    totalCount = 0,
                    publicCount = 0,
                    privateCount = 0,
                    loaded = true,
                ),
            onBack = {},
            onShowShop = {},
            onEditReview = { _, _ -> },
            onIntent = {},
        )
    }
}
