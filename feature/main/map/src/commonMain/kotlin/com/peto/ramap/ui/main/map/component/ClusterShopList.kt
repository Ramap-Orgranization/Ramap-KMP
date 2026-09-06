package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursStatusResourceMapper
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.main.map.config.MapInteractionConfig
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
internal fun ClusterShopList(
    shops: List<RamenShop>,
    operatingNotices: List<OperatingNotice>,
    onShopClick: (RamenShop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        shops.forEachIndexed { index, shop ->
            ClusterShopItem(
                shop = shop,
                currentDateTime = currentDateTime,
                operatingNotices = operatingNotices,
                onClick = { onShopClick(shop) },
            )
            if (index != shops.lastIndex) {
                HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
            }
        }
    }
}

@Composable
private fun ClusterShopItem(
    shop: RamenShop,
    currentDateTime: LocalDateTime,
    operatingNotices: List<OperatingNotice>,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (shop.isVisible) 1f else MapInteractionConfig.HIDDEN_SHOP_ALPHA)
                .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RemoteShopImage(
            url = shop.instagramProfileImageUrl,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AppText(
                text = shop.name,
                style = AppTextStyle.B1,
                color = ChromaticColor.Blue400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            shop.businessHoursStatus(currentDateTime, operatingNotices)?.let { status ->
                val statusText = BusinessHoursStatusResourceMapper.status(status).format()
                val highlightedLabel = BusinessHoursStatusResourceMapper.noticeLabel(status)?.format()
                if (highlightedLabel == null) {
                    AppText(
                        text = statusText,
                        style = AppTextStyle.B2,
                        color = GrayColor.C500,
                    )
                } else {
                    Row {
                        AppText(
                            text = highlightedLabel,
                            style = AppTextStyle.B4,
                            color = SystemColor.Warning,
                        )
                        AppText(
                            text = statusText.removePrefix(highlightedLabel),
                            style = AppTextStyle.B4,
                            color = GrayColor.C500,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClusterShopListPreview() {
    val shops = RamenShopPreviewParameterProvider().ramenShopPreviewSamples
    val notices = OperatingNoticePreviewParameterProvider().values.first()

    RamapTheme {
        ClusterShopList(
            shops = shops,
            operatingNotices = notices,
            onShopClick = {},
        )
    }
}
