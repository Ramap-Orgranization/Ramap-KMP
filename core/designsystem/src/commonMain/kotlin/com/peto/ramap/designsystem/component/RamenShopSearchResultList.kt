package com.peto.ramap.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
fun RamenShopSearchResultList(
    shops: RamenShops,
    onShopClick: (RamenShop) -> Unit,
    categoryLabel: @Composable (Category) -> String,
    modifier: Modifier = Modifier,
    itemModifier: (RamenShop) -> Modifier = { Modifier },
    showDividers: Boolean = true,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
    ) {
        shops.values.forEachIndexed { index, shop ->
            if (showDividers && index > 0) {
                HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
            }
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
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent()
            RemoteShopImage(
                url = shop.instagramProfileImageUrl,
                modifier =
                    Modifier
                        .size(44.dp)
                        .border(1.dp, GrayColor.C100, CircleShape)
                        .clip(CircleShape),
            )
            AppText(
                text = shop.name,
                modifier = Modifier.weight(1f),
                style = AppTextStyle.B1,
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
