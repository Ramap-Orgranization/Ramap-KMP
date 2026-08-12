package com.peto.ramap.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
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
private fun RamenShopSummariesPreview() {
    RamapTheme {
        RamenShopSummaries(
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
                            instagramUrl = null,
                            instagramProfileImageUrl = null,
                            menuCategories =
                                MenuCategories(
                                    listOf(
                                        Category.TONKOTSU,
                                        Category.TSUKEMEN,
                                    ),
                                ),
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
                            instagramUrl = null,
                            instagramProfileImageUrl = null,
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
