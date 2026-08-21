package com.peto.ramap.designsystem.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.EventBadge
import com.peto.ramap.designsystem.badge.EventDateBadge
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.operating_notice_source_action

@Composable
fun OperatingNoticeCard(
    notice: OperatingNotice,
    isSourceUrlSupported: (String) -> Boolean,
    onSourceClick: (String) -> Unit,
    onShopClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CommonColor.White),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RamenShopSummary(
            shop = notice.shop,
            onClick = { onShopClick(notice.shop.id) },
            categoryLabel = { category ->
                stringResource(CategoryResourceMapper.label(category))
            },
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                EventBadge(
                    text = stringResource(ShopOperatingNoticeResourceMapper.typeLabel(notice.type)),
                    textStyle = AppTextStyle.C2,
                )
                EventDateBadge(text = eventDateText("${notice.startDate}", "${notice.endDate}"))
            }

            AppText(
                text = notice.description,
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                overflow = TextOverflow.Ellipsis,
            )
            HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
            notice.sourceUrl?.takeIf(isSourceUrlSupported)?.let { sourceUrl ->
                AppText(
                    text = stringResource(Res.string.operating_notice_source_action),
                    style = AppTextStyle.B1,
                    color = GrayColor.C500,
                    modifier = Modifier.clickable { onSourceClick(sourceUrl) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun OperatingNoticeCardPreview(
    @PreviewParameter(OperatingNoticePreviewParameterProvider::class)
    notices: List<OperatingNotice>,
) {
    RamapTheme {
        OperatingNoticeCard(
            notice = notices.first(),
            isSourceUrlSupported = { true },
            onSourceClick = {},
            onShopClick = {},
        )
    }
}
