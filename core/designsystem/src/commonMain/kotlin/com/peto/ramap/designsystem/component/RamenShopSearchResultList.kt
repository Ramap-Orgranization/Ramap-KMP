package com.peto.ramap.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.search_result_count

@Composable
fun RamenShopSearchResultList(
    shops: RamenShops,
    onShopClick: (RamenShop) -> Unit,
    categoryLabel: @Composable (Category) -> String,
    modifier: Modifier = Modifier,
    itemModifier: (RamenShop) -> Modifier = { Modifier },
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

        shops.values.forEach { shop ->
            RamenShopSearchResultItem(
                shop = shop,
                onClick = { onShopClick(shop) },
                categoryLabel = categoryLabel,
                modifier = itemModifier(shop),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RamenShopSearchResultItem(
    shop: RamenShop,
    onClick: (() -> Unit)?,
    categoryLabel: @Composable (Category) -> String,
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            leadingContent()
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
                    MenuCategoryChip(label = categoryLabel(category))
                }
            }
        }
    }
}
