package com.peto.ramap.ui.main.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppFloatingActionButton
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.review.ReviewEmptyContent
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.preview.ReviewPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.feed.component.FeedReviewItem
import com.peto.ramap.ui.main.feed.component.FeedSkeleton
import com.peto.ramap.ui.main.feed.component.FeedSortSelector
import com.peto.ramap.ui.main.feed.contract.FeedIntent
import com.peto.ramap.ui.main.feed.contract.FeedUiState
import com.peto.ramap.ui.paging.ObserveLoadMoreNearListEnd
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_arrow_up
import ramap.shared.generated.resources.scroll_to_top
import ramap.shared.generated.resources.shop_review_empty_title
import ramap.shared.generated.resources.shop_review_load_failure_message
import ramap.shared.generated.resources.top_level_tab_feed

@Composable
internal fun FeedContent(
    state: FeedUiState,
    listState: LazyListState,
    onIntent: (FeedIntent) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenShop: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    LaunchedEffect(state.sort) {
        listState.scrollToItem(0)
    }
    ObserveLoadMoreNearListEnd(
        listState = listState,
        itemThreshold = PREFETCH_ITEM_THRESHOLD,
        hasMore = state.nextCursor != null,
        isLoading = state.appending,
        onLoadMore = { onIntent(FeedIntent.LoadMore) },
    )
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200
        }
    }
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CommonTopBar(title = stringResource(Res.string.top_level_tab_feed), left = {})
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                FeedSortSelector(
                    selected = state.sort,
                    onSelect = { onIntent(FeedIntent.SelectSort(it)) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(FeedIntent.Refresh) },
                modifier = Modifier.weight(1f),
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = state.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = CommonColor.White,
                        color = CommonColor.Black,
                    )
                },
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    if (state.isLoadingInitial) {
                        item(key = "initial_loading") {
                            FeedSkeleton(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (state.isEmpty) {
                        item(key = "empty") {
                            ReviewEmptyContent(
                                text = stringResource(Res.string.shop_review_empty_title),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                            )
                        }
                    }
                    items(items = state.reviews, key = { it.review.id }) { row ->
                        FeedReviewItem(
                            row = row,
                            state = state,
                            onIntent = onIntent,
                            onOpenProfile = onOpenProfile,
                            onOpenShop = onOpenShop,
                            onEditReview = onEditReview,
                        )
                    }
                    if (state.hasError || state.hasAppendError) {
                        item(key = "load_failure") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AppText(
                                    text = stringResource(Res.string.shop_review_load_failure_message),
                                    style = AppTextStyle.B1,
                                    color = GrayColor.C300,
                                )
                                RetryButton(onClick = { onIntent(FeedIntent.Retry) })
                            }
                        }
                    }
                    if (state.appending) {
                        item(key = "append_loading") {
                            RamenLoadingIndicator(modifier = Modifier.fillMaxWidth().height(60.dp))
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
        ) {
            AppFloatingActionButton(
                icon = Res.drawable.ic_arrow_up,
                contentDescription = stringResource(Res.string.scroll_to_top),
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = CommonColor.White,
            )
        }
    }
}

@Preview(name = "피드 피드", showBackground = true)
@Composable
private fun FeedContentPreview() {
    val reviews =
        ReviewPreviewParameterProvider().reviewPreviewSamples.mapIndexed { index, review ->
            ProfileReview(
                review = review,
                shop = RamenShopPreviewParameterProvider().ramenShopPreviewSamples.let { it[index % it.size] }.copy(id = review.shopId),
            )
        }
    RamapTheme {
        FeedContent(
            state =
                FeedUiState(
                    reviews = reviews,
                    hasLoaded = true,
                    sessionResolved = true,
                    currentUserId = "my-user-id",
                ),
            listState = LazyListState(),
            onIntent = {},
            onOpenProfile = {},
            onOpenShop = {},
            onEditReview = { _, _ -> },
        )
    }
}

@Preview(name = "피드 로딩", showBackground = true)
@Composable
private fun FeedContentLoadingPreview() {
    RamapTheme {
        FeedContent(
            state =
                FeedUiState(
                    hasLoaded = false,
                    sessionResolved = false,
                ),
            listState = LazyListState(),
            onIntent = {},
            onOpenProfile = {},
            onOpenShop = {},
            onEditReview = { _, _ -> },
        )
    }
}

@Preview(name = "피드 비어있음", showBackground = true)
@Composable
private fun FeedContentEmptyPreview() {
    RamapTheme {
        FeedContent(
            state =
                FeedUiState(
                    reviews = emptyList(),
                    hasLoaded = true,
                    sessionResolved = true,
                ),
            listState = LazyListState(),
            onIntent = {},
            onOpenProfile = {},
            onOpenShop = {},
            onEditReview = { _, _ -> },
        )
    }
}

@Preview(name = "피드 오류", showBackground = true)
@Composable
private fun FeedContentFailurePreview() {
    RamapTheme {
        FeedContent(
            state =
                FeedUiState(
                    reviews = emptyList(),
                    hasLoaded = true,
                    sessionResolved = true,
                    failed = true,
                ),
            listState = LazyListState(),
            onIntent = {},
            onOpenProfile = {},
            onOpenShop = {},
            onEditReview = { _, _ -> },
        )
    }
}

private const val PREFETCH_ITEM_THRESHOLD = 3
