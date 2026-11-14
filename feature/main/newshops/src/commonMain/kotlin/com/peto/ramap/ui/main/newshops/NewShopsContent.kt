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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.ShopsEmptyContent
import com.peto.ramap.designsystem.component.Skeleton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.domain.model.shop.ShopOpening
import com.peto.ramap.domain.model.shop.ShopOpeningMonth
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.newshops.component.NEW_SHOP_CARD_RATIO
import com.peto.ramap.ui.main.newshops.component.NewShopCard
import com.peto.ramap.ui.main.newshops.contract.NewShopsUiState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_error_description
import ramap.shared.generated.resources.event_list_error_title
import ramap.shared.generated.resources.new_shops_empty
import ramap.shared.generated.resources.new_shops_month_count
import ramap.shared.generated.resources.news_load_error_illustration
import kotlin.time.Instant

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
            val fullText = stringResource(Res.string.new_shops_month_count, uiState.month.monthNumber, uiState.totalShopCount)
            val highlightText = "${uiState.totalShopCount}개"
            val annotatedText =
                remember(fullText, highlightText) {
                    buildAnnotatedString {
                        val startIndex = fullText.indexOf(highlightText)
                        if (startIndex != -1) {
                            append(fullText.substring(0, startIndex))
                            withStyle(SpanStyle(color = ChromaticColor.Orange400)) {
                                append(highlightText)
                            }
                            append(fullText.substring(startIndex + highlightText.length))
                        } else {
                            append(fullText)
                        }
                    }
                }
            AppText(
                text = annotatedText,
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

@Preview(showBackground = true)
@Composable
private fun NewShopsContentPreview() {
    RamapTheme {
        NewShopsContent(
            uiState =
                NewShopsUiState(
                    month = ShopOpeningMonth.containing(Instant.parse("2026-11-14T00:00:00Z")),
                    shops =
                        listOf(
                            NewShop(
                                id = "1",
                                name = "고고토리",
                                opening = ShopOpening(date = LocalDate(2026, 11, 10), kind = ShopOpening.Kind.SOFT),
                                coverImageUrl = null,
                            ),
                            NewShop(
                                id = "2",
                                name = "라멘키료쿠",
                                opening = ShopOpening(date = LocalDate(2026, 11, 9), kind = ShopOpening.Kind.OFFICIAL),
                                coverImageUrl = null,
                            ),
                            NewShop(
                                id = "3",
                                name = "쿠지라스토랑 울산점",
                                opening = ShopOpening(date = LocalDate(2026, 11, 3), kind = ShopOpening.Kind.OFFICIAL),
                                coverImageUrl = null,
                            ),
                        ),
                ),
            onRefresh = {},
            onRetry = {},
            onShopClick = {},
        )
    }
}
