package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.component.ShopBookmarkIcon
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.ui.main.map.model.search.SearchResultGuide
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.ranking_bookmark_cancel_action
import ramap.shared.generated.resources.retry_action

@Composable
internal fun RecommendationList(
    shops: List<RamenShop>,
    bookmarkedShopIds: Set<String>,
    bookmarkUpdatingShopIds: Set<String>,
    isLoading: Boolean,
    hasLoadFailed: Boolean,
    listState: LazyListState,
    onRetry: () -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onBookmarkClick: (RamenShop, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RecommendationListHeader(modifier = Modifier.heightIn(min = 100.dp))

        val listModifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp)

        when {
            isLoading -> RecommendationListSkeleton(modifier = listModifier)

            hasLoadFailed ->
                AppButton(
                    text = stringResource(Res.string.retry_action),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                    onClick = onRetry,
                )

            shops.isEmpty() -> SearchResultGuide(guide = SearchResultGuide.FilterEmpty)

            else ->
                LazyColumn(
                    state = listState,
                    modifier = listModifier,
                ) {
                    itemsIndexed(shops, key = { _, shop -> shop.id }) { _, shop ->
                        val isBookmarked = shop.id in bookmarkedShopIds
                        val isBookmarkEnabled = shop.id !in bookmarkUpdatingShopIds
                        val bookmarkDescription =
                            stringResource(
                                if (isBookmarked) {
                                    Res.string.ranking_bookmark_cancel_action
                                } else {
                                    Res.string.bookmarked_shops_toggle
                                },
                            )
                        RamenShopSummary(
                            shop = shop,
                            onClick = { onShopClick(shop) },
                            modifier = Modifier.padding(vertical = 5.dp),
                            trailingContent = {
                                Box(
                                    modifier =
                                        Modifier
                                            .align(Alignment.CenterEnd)
                                            .size(48.dp)
                                            .noRippleClickable(
                                                enabled = isBookmarkEnabled,
                                                onClick = { onBookmarkClick(shop, isBookmarked) },
                                            ).semantics {
                                                contentDescription = bookmarkDescription
                                            },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ShopBookmarkIcon(isBookmarked = isBookmarked)
                                }
                            },
                        )
                    }
                }
        }
    }
}
