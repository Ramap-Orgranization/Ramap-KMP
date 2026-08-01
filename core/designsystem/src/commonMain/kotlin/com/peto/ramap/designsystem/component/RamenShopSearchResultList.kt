package com.peto.ramap.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_favorite_border

private val SearchResultCardShape = RoundedCornerShape(8.dp)

@Composable
fun RamenShopSearchResultList(
    shops: RamenShops,
    onShopClick: (RamenShop) -> Unit,
    categoryLabel: @Composable (Category) -> String,
    modifier: Modifier = Modifier,
    itemModifier: (RamenShop) -> Modifier = {
        Modifier.padding(
            horizontal = 12.dp,
            vertical = 6.dp,
        )
    },
    itemActionLabel: (@Composable (RamenShop) -> String)? = null,
    onItemAction: ((RamenShop) -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
    ) {
        shops.values.forEach { shop ->
            RamenShopSearchResultItem(
                shop = shop,
                onClick = { onShopClick(shop) },
                categoryLabel = categoryLabel,
                modifier = itemModifier(shop),
                actionLabel = itemActionLabel?.invoke(shop),
                onAction = onItemAction?.let { action -> { action(shop) } },
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
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.noRippleClickable(onClick = onClick) else Modifier)
                .border(1.dp, GrayColor.C100, SearchResultCardShape)
                .clip(SearchResultCardShape)
                .background(CommonColor.White),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .padding(end = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent()
            RemoteShopImage(
                url = shop.instagramProfileImageUrl,
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(8.dp),
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
                if (shop.hasCategory) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        shop.menuCategories.forEach { category ->
                            CategoryFilterChip(
                                label = categoryLabel(category),
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }
            }
        }

        SearchResultItemAction(
            shop = shop,
            actionLabel = actionLabel,
            onAction = onAction,
        )
    }
}

@Composable
private fun BoxScope.SearchResultItemAction(
    shop: RamenShop,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    if (actionLabel != null && onAction != null) {
        IconButton(
            onClick = onAction,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(24.dp)
                    .semantics {
                        contentDescription = "${shop.name} $actionLabel"
                    },
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_close),
                contentDescription = null,
                tint = GrayColor.C400,
            )
        }
        return
    }

    Icon(
        painter = painterResource(Res.drawable.ic_favorite_border),
        contentDescription = null,
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(24.dp),
        tint = GrayColor.C400,
    )
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
