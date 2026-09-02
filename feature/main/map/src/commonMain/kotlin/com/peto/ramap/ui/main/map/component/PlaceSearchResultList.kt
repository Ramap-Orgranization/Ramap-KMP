package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursStatusResourceMapper
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.main.map.config.MapInteractionConfig
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_search_result_break_time_label
import ramap.shared.generated.resources.map_search_result_closed_label
import kotlin.time.Clock

@Composable
internal fun SearchResultList(
    shops: RamenShops,
    operatingNotices: List<OperatingNotice>,
    onShopClick: (RamenShop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        shops.values.forEachIndexed { index, shop ->
            SearchResultItem(
                shop = shop,
                currentDateTime = currentDateTime,
                operatingNotices = operatingNotices,
                onClick = { onShopClick(shop) },
            )
            if (index != shops.size - 1) {
                HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    shop: RamenShop,
    currentDateTime: LocalDateTime,
    operatingNotices: List<OperatingNotice>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (shop.isVisible) 1f else MapInteractionConfig.HIDDEN_SHOP_ALPHA)
                .clickable(onClick = onClick)
                .padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RemoteShopImage(
                url = shop.instagramProfileImageUrl,
                modifier = Modifier.size(70.dp),
                shape = RoundedCornerShape(8.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                AppText(
                    text = shop.name,
                    style = AppTextStyle.T3,
                    color = ChromaticColor.Blue400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                shop.businessHoursStatus(currentDateTime, operatingNotices)?.let { status ->
                    val statusText = BusinessHoursStatusResourceMapper.status(status).format()
                    val highlightedLabel =
                        when (status) {
                            is BusinessHoursStatus.BreakTime ->
                                stringResource(Res.string.map_search_result_break_time_label)

                            is BusinessHoursStatus.Closed ->
                                stringResource(Res.string.map_search_result_closed_label)

                            else -> null
                        }
                    if (highlightedLabel != null) {
                        Row {
                            AppText(
                                text = highlightedLabel,
                                style = AppTextStyle.B2,
                                color = SystemColor.Warning,
                            )
                            AppText(
                                text = statusText.removePrefix(highlightedLabel),
                                style = AppTextStyle.B2,
                                color = GrayColor.C500,
                            )
                        }
                    } else {
                        AppText(
                            text = statusText,
                            style = AppTextStyle.B2,
                            color = GrayColor.C500,
                        )
                    }
                }
                AppText(
                    text = shop.address,
                    style = AppTextStyle.B2,
                    color = GrayColor.C300,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchResultListPreview(
    @PreviewParameter(RamenShopsPreviewParameterProvider::class) shops: RamenShops,
) {
    RamapTheme {
        SearchResultList(
            shops = shops,
            operatingNotices = emptyList(),
            onShopClick = {},
        )
    }
}
