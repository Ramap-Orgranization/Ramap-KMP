package com.peto.ramap.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right

@Composable
fun RamenShopCompactSummary(
    shop: RamenShop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RamenShopCompactSummary(
        shopName = shop.name,
        address = shop.address,
        profileImageUrl = shop.instagramProfileImageUrl,
        onClick = onClick,
        modifier =
            modifier
                .background(ChromaticColor.Orange050)
                .padding(8.dp),
        chevronColor = ChromaticColor.Orange300,
    )
}

@Composable
fun RamenShopCompactSummary(
    shopName: String,
    address: String,
    profileImageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chevronColor: Color = GrayColor.C500,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteShopImage(
                url = profileImageUrl,
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AppText(
                    text = shopName,
                    style = AppTextStyle.B1,
                    color = GrayColor.C500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AppText(
                    text = address,
                    style = AppTextStyle.C1,
                    color = GrayColor.C300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Image(
                painter = painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(chevronColor),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RamenShopCompactSummaryPreview(
    @PreviewParameter(RamenShopPreviewParameterProvider::class) shop: RamenShop,
) {
    RamapTheme {
        RamenShopCompactSummary(
            shop = shop,
            onClick = {},
        )
    }
}
