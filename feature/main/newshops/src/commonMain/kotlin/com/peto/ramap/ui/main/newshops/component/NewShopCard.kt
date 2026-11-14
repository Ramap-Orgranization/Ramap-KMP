package com.peto.ramap.ui.main.newshops.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.resource.shop.ShopOpeningResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.domain.model.shop.ShopOpening
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.DimmedColor
import com.peto.ramap.theme.RamapTheme
import kotlinx.datetime.LocalDate

@Composable
internal fun NewShopCard(
    shop: NewShop,
    onClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val minHeight = (maxWidth / NEW_SHOP_CARD_RATIO).coerceAtLeast(MinCardHeight)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onClick),
        ) {
            if (shop.coverImageUrl != null) {
                AsyncImage(
                    model = shop.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.5f to DimmedColor.D020,
                                        1f to DimmedColor.D070,
                                    ),
                            )
                        }.padding(start = 16.dp, top = CaptionFadeHeight, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AppText(text = shop.name, style = AppTextStyle.T1, color = CommonColor.White)
                AppText(
                    text = ShopOpeningResourceMapper.openingDate(shop.opening).format(),
                    style = AppTextStyle.B2,
                    color = CommonColor.White,
                )
            }
        }
    }
}

private val MinCardHeight = 128.dp
internal const val NEW_SHOP_CARD_RATIO = 2.35f
private val CaptionFadeHeight = 32.dp

@Preview(showBackground = true)
@Composable
private fun NewShopCardPreview() {
    RamapTheme {
        NewShopCard(
            shop =
                NewShop(
                    id = "preview_1",
                    name = "유기라멘",
                    opening = ShopOpening(date = LocalDate(2026, 10, 28), kind = ShopOpening.Kind.OPENING),
                    coverImageUrl = null,
                ),
            onClick = {},
        )
    }
}
