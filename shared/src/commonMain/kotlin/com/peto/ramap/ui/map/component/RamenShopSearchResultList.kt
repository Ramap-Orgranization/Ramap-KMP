package com.peto.ramap.ui.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.core.extension.noRippleClickable
import com.peto.ramap.core.extension.stringResource
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.search_result_count

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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
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
                .noRippleClickable(onClick = onClick)
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

    HorizontalDivider(
        thickness = 1.dp,
        color = GrayColor.C100,
    )
}
