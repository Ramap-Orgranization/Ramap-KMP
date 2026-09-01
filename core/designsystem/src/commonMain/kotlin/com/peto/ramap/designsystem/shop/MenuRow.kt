package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.menu.Menu
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.MenuSectionsPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_menu_price
import ramap.shared.generated.resources.shop_menu_representative
import ramap.shared.generated.resources.shop_menu_source

@Composable
internal fun MenuRow(
    item: Menu,
    onSourceClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (item.isRepresentative) {
                NewsBadge(
                    text = stringResource(Res.string.shop_menu_representative),
                    modifier =
                        Modifier
                            .border(
                                1.dp,
                                GrayColor.C200,
                                RoundedCornerShape(999.dp),
                            ),
                    textStyle = AppTextStyle.C2,
                    containerColor = CommonColor.White,
                    contentColor = GrayColor.C400,
                )
            }
            AppText(
                text = item.name,
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
            item.description?.let { description ->
                AppText(
                    text = description,
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                )
            }
            menuPriceLabel(item)?.let { priceLabel ->
                AppText(
                    text = priceLabel,
                    modifier = Modifier.padding(top = 4.dp),
                    style = AppTextStyle.T2,
                    color = GrayColor.C500,
                )
            }
            item.sourceUrl?.let { sourceUrl ->
                AppText(
                    text = stringResource(Res.string.shop_menu_source),
                    modifier = Modifier.padding(top = 4.dp).noRippleClickable { onSourceClick(sourceUrl) },
                    style = AppTextStyle.B1,
                    color = GrayColor.C300,
                )
            }
        }
        item.imageUrl?.let { imageUrl ->
            RemoteShopImage(
                url = imageUrl,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

@Composable
private fun menuPriceLabel(item: Menu): String? = item.priceKrw?.let { stringResource(Res.string.shop_menu_price, it.formatted) } ?: item.priceText

@Preview(showBackground = true)
@Composable
private fun MenuRowPreview(
    @PreviewParameter(MenuSectionsPreviewParameterProvider::class) sections: List<MenuSection>,
) {
    RamapTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            sections.flatMap { it.items }.forEach { item ->
                MenuRow(item = item, onSourceClick = {})
            }
        }
    }
}
