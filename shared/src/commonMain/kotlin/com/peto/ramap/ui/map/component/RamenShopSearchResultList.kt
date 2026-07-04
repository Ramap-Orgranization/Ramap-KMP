package com.peto.ramap.ui.map.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.core.extension.stringResource
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.map.contract.SearchResultGuide
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.search_result_count
import ramap.shared.generated.resources.search_result_empty_message
import ramap.shared.generated.resources.search_result_filter_empty_message
import ramap.shared.generated.resources.search_result_hidden_only_message
import ramap.shared.generated.resources.search_result_query_filter_empty_message

@Composable
fun RamenShopSearchResultList(
    shops: List<RamenShop>,
    onShopClick: (RamenShop) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
    ) {
        AppText(
            text = stringResource(Res.string.search_result_count, shops.size),
            modifier = Modifier.padding(horizontal = 24.dp),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
        )

        shops.forEach { shop ->
            RamenShopSearchResultItem(
                shop = shop,
                onClick = { onShopClick(shop) },
            )
        }
    }
}

@Composable
fun RamenShopSearchResultGuide(
    guide: SearchResultGuide,
    modifier: Modifier = Modifier,
) {
    val message =
        when (guide) {
            SearchResultGuide.SEARCH_EMPTY -> stringResource(Res.string.search_result_empty_message)
            SearchResultGuide.FILTER_EMPTY -> stringResource(Res.string.search_result_filter_empty_message)
            SearchResultGuide.QUERY_AND_FILTER_EMPTY ->
                stringResource(Res.string.search_result_query_filter_empty_message)
            SearchResultGuide.HIDDEN_ONLY -> stringResource(Res.string.search_result_hidden_only_message)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = message,
            style = AppTextStyle.B1,
            color = GrayColor.C400,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RamenShopSearchResultItem(
    shop: RamenShop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (shop.isVisible) 1f else MapInteractionConfig.HIDDEN_SHOP_ALPHA)
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AppText(
                text = shop.name,
                modifier = Modifier.weight(1f),
                style = AppTextStyle.H3,
                color = GrayColor.C500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AppText(
            text = shop.address,
            style = AppTextStyle.B2,
            color = GrayColor.C300,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (shop.hasCategory) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shop.menuCategories.forEach { category ->
                    MenuCategoryChip(label = stringResource(category.stringResource))
                }
            }
        }
    }
}
