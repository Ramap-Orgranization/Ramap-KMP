package com.peto.ramap.ui.main.newshops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.ShopsEmptyContent
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.newshops.component.NEW_SHOP_CARD_RATIO
import com.peto.ramap.ui.main.newshops.component.NewShopCard
import com.peto.ramap.ui.main.newshops.contract.NewShopsUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_error_description
import ramap.shared.generated.resources.event_list_error_title
import ramap.shared.generated.resources.new_shops_empty
import ramap.shared.generated.resources.new_shops_month_count
import ramap.shared.generated.resources.news_load_error_illustration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewShopsContent(
    uiState: NewShopsUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onShopClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.totalShopCount > 0) {
            AppText(
                text = stringResource(Res.string.new_shops_month_count, uiState.month.monthNumber, uiState.totalShopCount),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .semantics { heading() },
            )
        }
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = CommonColor.White,
                    color = CommonColor.Black,
                )
            },
        ) {
            val shops = uiState.shops.orEmpty()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    uiState.isLoading || (!uiState.failed && uiState.shops == null) -> {
                        items(SKELETON_CARD_COUNT) {
                            Skeleton(
                                modifier = Modifier.fillMaxWidth().aspectRatio(NEW_SHOP_CARD_RATIO),
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                    }

                    uiState.failed -> {
                        item {
                            LoadErrorContent(
                                image = Res.drawable.news_load_error_illustration,
                                title = stringResource(Res.string.event_list_error_title),
                                description = stringResource(Res.string.event_list_error_description),
                                modifier = Modifier.fillMaxWidth(),
                                onRetry = onRetry,
                            )
                        }
                    }

                    shops.isEmpty() -> {
                        item {
                            ShopsEmptyContent(title = stringResource(Res.string.new_shops_empty), titleStyle = AppTextStyle.T1)
                        }
                    }

                    else ->
                        items(shops, key = NewShop::id) { shop ->
                            NewShopCard(shop = shop, onClick = { onShopClick(shop.id) })
                        }
                }
            }
        }
    }
}

private const val SKELETON_CARD_COUNT = 3
