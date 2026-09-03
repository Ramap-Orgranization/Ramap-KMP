package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursResourceMapper
import com.peto.ramap.designsystem.resource.businesshours.BusinessHoursStatusResourceMapper
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.BusinessHoursPreviewParameterProvider
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_keyboard_arrow_down
import kotlin.time.Clock

@Composable
internal fun BusinessHoursCard(
    shop: RamenShop,
    operatingNotice: OperatingNotice?,
    modifier: Modifier = Modifier,
    onOperatingNoticeClick: (OperatingNotice) -> Unit = {},
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val status = shop.businessHoursStatus(currentDateTime, listOfNotNull(operatingNotice))
    val lines = BusinessHoursResourceMapper.all(shop.businessHoursDetails ?: return)

    Column(
        modifier =
            modifier
                .border(
                    width = 1.dp,
                    color = GrayColor.C100,
                    shape = RoundedCornerShape(12.dp),
                ).background(
                    color = GrayColor.C050,
                    shape = RoundedCornerShape(12.dp),
                ).padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        BusinessHoursNotice()
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BusinessHoursStatusSummary(
                status = status,
                operatingNotice = operatingNotice,
                onOperatingNoticeClick = onOperatingNoticeClick,
                modifier = Modifier.padding(vertical = 5.dp),
            )

            Icon(
                painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
                modifier =
                    Modifier
                        .rotate(if (isExpanded) 180f else 0f)
                        .noRippleClickable(onClick = { isExpanded = !isExpanded }),
                tint = GrayColor.C400,
            )
        }

        if (isExpanded) {
            SectionCard {
                lines.forEachIndexed { index, line ->
                    BusinessHoursCardRow(
                        line = line,
                        isLast = index == lines.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessHoursStatusSummary(
    status: BusinessHoursStatus?,
    operatingNotice: OperatingNotice?,
    onOperatingNoticeClick: (OperatingNotice) -> Unit,
    modifier: Modifier,
) {
    if (operatingNotice != null) {
        val noticeText = ShopOperatingNoticeResourceMapper.notice(operatingNotice).format()
        AppText(
            text = noticeText,
            modifier = modifier.noRippleClickable { onOperatingNoticeClick(operatingNotice) },
            style = AppTextStyle.B1,
            color = SystemColor.Warning,
        )
        return
    }

    val statusText = status?.let(BusinessHoursStatusResourceMapper::status)?.format()
    val highlightedLabel = status?.let(BusinessHoursStatusResourceMapper::noticeLabel)?.format()

    if (statusText == null) {
        Spacer(modifier = modifier)
    } else if (highlightedLabel == null) {
        AppText(
            text = statusText,
            modifier = modifier,
            style = AppTextStyle.B2,
            color = GrayColor.C400,
        )
    } else {
        Row(modifier = modifier) {
            AppText(
                text = highlightedLabel,
                style = AppTextStyle.B1,
                color = SystemColor.Warning,
            )
            AppText(
                text = statusText.removePrefix(highlightedLabel),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BusinessHoursCardPreview(
    @PreviewParameter(BusinessHoursPreviewParameterProvider::class) businessHours: BusinessHours,
) {
    RamapTheme {
        BusinessHoursCard(
            shop =
                RamenShopPreviewParameterProvider()
                    .values
                    .first()
                    .copy(businessHoursDetails = businessHours),
            operatingNotice = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BusinessHoursCardWithNoticePreview() {
    val shop = RamenShopPreviewParameterProvider().values.first()
    RamapTheme {
        BusinessHoursCard(
            shop = shop,
            operatingNotice =
                OperatingNotice(
                    id = "1",
                    shop = shop,
                    type = OperatingNoticeType.TEMPORARY_CLOSURE,
                    description = "임시 휴무 안내입니다.",
                    startDate = LocalDate(2024, 1, 1),
                    endDate = null,
                    startTime = null,
                    endTime = null,
                    sourceUrl = null,
                ),
        )
    }
}
