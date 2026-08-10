package com.peto.ramap.ui.main.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.domain.model.rank.RankedShops
import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.ShopRanking
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.main.ranking.component.RankingEmptyContent
import com.peto.ramap.ui.main.ranking.component.RankingNextPageFooter
import com.peto.ramap.ui.main.ranking.component.RankingRow
import com.peto.ramap.ui.main.ranking.component.RankingSkeleton
import com.peto.ramap.ui.main.ranking.contract.RankingLoadKey
import com.peto.ramap.ui.main.ranking.contract.RankingUiState
import com.peto.ramap.ui.preview.RamenShopPreviewParameterProvider
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.ranking_error_description
import ramap.shared.generated.resources.ranking_error_title

@Composable
internal fun RankingContent(
    uiState: RankingUiState,
    listState: LazyListState,
    onShopClick: (RamenShop) -> Unit,
    onFindShopClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadNext: () -> Unit,
    onRetryNext: () -> Unit,
    onBookmarkClick: (RamenShop, Boolean) -> Unit,
) {
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = CommonColor.White,
                color = CommonColor.Black,
            )
        },
    ) {
        when {
            uiState.isLoading -> RankingSkeleton(Modifier.fillMaxSize())
            uiState.showError ->
                LoadErrorContent(
                    image = Res.drawable.laduck_error_crying,
                    title = stringResource(Res.string.ranking_error_title),
                    description = stringResource(Res.string.ranking_error_description),
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )

            uiState.shops.isEmpty() -> RankingEmptyContent(onFindShopClick)
            else ->
                LazyColumn(
                    state = listState,
                    overscrollEffect = null,
                    contentPadding = PaddingValues(start = 24.dp, end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(uiState.shops, key = { it.ranking.shop.id }) { item ->
                        RankingRow(
                            item = item,
                            isBookmarked = item.ranking.shop.id in uiState.bookmarkedShopIds,
                            isBookmarkEnabled =
                                item.ranking.shop.id !in uiState.bookmarkUpdatingShopIds,
                            likeCount = uiState.displayedLikeCount(item),
                            onBookmarkClick = {
                                onBookmarkClick(
                                    item.ranking.shop,
                                    item.ranking.shop.id in uiState.bookmarkedShopIds,
                                )
                            },
                            onClick = { onShopClick(item.ranking.shop) },
                        )
                    }
                    if (uiState.hasNext || uiState.isLoadingNext || uiState.showNextPageError) {
                        item {
                            RankingNextPageFooter(
                                uiState = uiState,
                                onLoadNext = onLoadNext,
                                onRetry = onRetryNext,
                            )
                        }
                    }
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingContentNextPageAvailablePreview() {
    RankingContentPreview(uiState = rankingContentPreviewState())
}

@Preview(showBackground = true)
@Composable
private fun RankingContentNextPageLoadingPreview() {
    RankingContentPreview(
        uiState = rankingContentPreviewState(loadState = LoadState.loading(RankingLoadKey.NextPage)),
    )
}

@Preview(showBackground = true)
@Composable
private fun RankingContentNextPageErrorPreview() {
    RankingContentPreview(
        uiState = rankingContentPreviewState(showNextPageError = true),
    )
}

@Composable
private fun RankingContentPreview(uiState: RankingUiState) {
    RamapTheme {
        RankingContent(
            uiState = uiState,
            listState = LazyListState(),
            onShopClick = {},
            onFindShopClick = {},
            onRefresh = {},
            onRetry = {},
            onLoadNext = {},
            onRetryNext = {},
            onBookmarkClick = { _, _ -> },
        )
    }
}

private fun rankingContentPreviewState(
    loadState: LoadState = LoadState(),
    showNextPageError: Boolean = false,
): RankingUiState {
    val shops = RamenShopPreviewParameterProvider().ramenShopPreviewSamples
    val rankings =
        shops.mapIndexed { index, shop ->
            ShopRanking(
                shop = shop,
                likeCount = 128L - (index * 33L),
            )
        }

    return RankingUiState(
        shops = RankedShops(ShopRankings(rankings)),
        nextCursor = RankingCursor(likeCount = 95, name = "라멘 트럭", shopId = "2"),
        bookmarkedShopIds = setOf(shops.first().id),
        showNextPageError = showNextPageError,
        loadState = loadState,
    )
}
