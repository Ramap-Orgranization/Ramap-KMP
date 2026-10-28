package com.peto.ramap.ui.review.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppIconButton
import com.peto.ramap.designsystem.component.AppSearchBar
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchIntent
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.find_visited_shop
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.recent_viewed_shops_title
import ramap.shared.generated.resources.search_bar_placeholder
import ramap.shared.generated.resources.search_empty_illustration
import ramap.shared.generated.resources.search_result_empty_message

@Composable
internal fun ReviewShopSearchContent(
    state: ReviewShopSearchUiState,
    onBack: () -> Unit,
    onIntent: (ReviewShopSearchIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        CommonTopBar(
            title = stringResource(Res.string.find_visited_shop),
            left = {
                AppIconButton(
                    icon = Res.drawable.ic_arrow3_left,
                    contentDescription = stringResource(Res.string.navigation_back),
                    onClick = onBack,
                )
            },
        )
        AppSearchBar(
            query = state.query,
            onQueryChange = { onIntent(ReviewShopSearchIntent.QueryChanged(it)) },
            onSearch = { onIntent(ReviewShopSearchIntent.Search) },
            modifier = Modifier.padding(16.dp),
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                state.isLoading -> RamenLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                state.hasError ->
                    LoadErrorContent(
                        image = Res.drawable.laduck_error_confused,
                        title = stringResource(Res.string.data_load_failure_message),
                        onRetry = { onIntent(ReviewShopSearchIntent.Retry) },
                        modifier = Modifier.fillMaxSize(),
                    )
                state.shops.isNotEmpty() ->
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (state.isShowingHistory) {
                            item {
                                AppText(text = stringResource(Res.string.recent_viewed_shops_title), style = AppTextStyle.T2, color = GrayColor.C500)
                            }
                        }
                        items(items = state.shops.values.toList(), key = { it.id }) { shop ->
                            RamenShopSummary(
                                shop = shop,
                                onClick = { onIntent(ReviewShopSearchIntent.SelectShop(shop.id)) },
                            )
                        }
                    }
                else ->
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.search_empty_illustration),
                            contentDescription = null,
                            modifier = Modifier.size(160.dp),
                        )
                        AppText(
                            text = stringResource(if (state.hasSearched) Res.string.search_result_empty_message else Res.string.search_bar_placeholder),
                            style = AppTextStyle.B2,
                            color = GrayColor.C300,
                        )
                    }
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun ReviewShopSearchContentPreview() {
    RamapTheme {
        ReviewShopSearchContent(
            state = ReviewShopSearchUiState(recentShops = RamenShopsPreviewParameterProvider().values.first()),
            onBack = {},
            onIntent = {},
        )
    }
}
