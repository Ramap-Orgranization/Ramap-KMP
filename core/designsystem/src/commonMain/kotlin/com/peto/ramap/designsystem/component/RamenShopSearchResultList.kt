package com.peto.ramap.designsystem.component

import androidx.compose.foundation.border
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

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
private fun RamenShopSearchResultItem(
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
                .then(if (onClick != null) Modifier.noRippleClickable(onClick = onClick) else Modifier)
                .padding(12.dp),
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AppText(
                    text = shop.name,
                    style = AppTextStyle.B1,
                    color = GrayColor.C500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AppText(
                    text = shop.address,
                    style = AppTextStyle.B2,
                    color = GrayColor.C300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (shop.hasCategory) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shop.menuCategories.forEach { category ->
                    CategoryFilterChip(
                        label = categoryLabel(category),
                        selected = false,
                        onClick = {},
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RamenShopSearchResultListPreview() {
    RamapTheme {
        RamenShopSearchResultList(
            shops =
                RamenShops(
                    listOf(
                        RamenShop(
                            id = "1",
                            kakaoPlaceId = null,
                            name = "멘야 하나비",
                            address = "서울 강남구 테헤란로 123",
                            location = Location(lat = 37.5, lng = 127.0),
                            kakaoPlaceUrl = null,
                            phone = null,
                            businessHours = null,
                            instagramUrl = null,
                            instagramProfileImageUrl = null,
                            kakaoRating = 4.5,
                            menuCategories = MenuCategories(listOf(Category.TONKOTSU, Category.TSUKEMEN)),
                            isVisible = true,
                            createdAt = "",
                            updatedAt = "",
                        ),
                        RamenShop(
                            id = "2",
                            kakaoPlaceId = null,
                            name = "라멘 지로",
                            address = "서울 마포구 양화로 45",
                            location = Location(lat = 37.55, lng = 126.9),
                            kakaoPlaceUrl = null,
                            phone = null,
                            businessHours = null,
                            instagramUrl = null,
                            instagramProfileImageUrl = null,
                            kakaoRating = 4.2,
                            menuCategories = MenuCategories(emptyList()),
                            isVisible = true,
                            createdAt = "",
                            updatedAt = "",
                        ),
                    ),
                ),
            onShopClick = {},
            categoryLabel = { category -> category.name },
        )
    }
}
