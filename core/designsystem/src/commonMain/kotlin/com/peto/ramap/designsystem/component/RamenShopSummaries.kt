package com.peto.ramap.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme

@Composable
fun RamenShopSummaries(
    shops: RamenShops,
    onShopClick: (RamenShop) -> Unit,
    categoryLabel: @Composable (Category) -> String,
    modifier: Modifier = Modifier,
    itemActionLabel: (@Composable (RamenShop) -> String)? = null,
    onItemAction: ((RamenShop) -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        shops.values.forEach { shop ->
            RamenShopSummary(
                shop = shop,
                onClick = { onShopClick(shop) },
                categoryLabel = categoryLabel,
                actionLabel = itemActionLabel?.invoke(shop),
                onAction = onItemAction?.let { action -> { action(shop) } },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RamenShopSummariesPreview(
    @PreviewParameter(RamenShopsPreviewParameterProvider::class) shops: RamenShops,
) {
    RamapTheme {
        RamenShopSummaries(
            shops = shops,
            onShopClick = {},
            categoryLabel = { category -> category.name },
        )
    }
}
