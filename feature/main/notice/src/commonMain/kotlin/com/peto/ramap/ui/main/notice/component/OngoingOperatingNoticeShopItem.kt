package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.operatingnotice.OperatingNoticeResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.OperatingNoticePreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.operating_notice_closing_time

@Composable
internal fun OngoingOperatingNoticeShop(
    notice: OperatingNotice,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RemoteShopImage(
            url = notice.shop.instagramProfileImageUrl,
            modifier = Modifier.size(56.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppText(
                text = notice.shop.name,
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                NewsBadge(
                    text = stringResource(OperatingNoticeResourceMapper.typeLabel(notice.type)),
                    textStyle = AppTextStyle.C2,
                    containerColor = OperatingNoticeResourceMapper.badgeContainerColor(notice.type),
                    contentColor = CommonColor.White,
                )
                if (notice.type.hasClosingTime) {
                    notice.endTime?.let { time ->
                        AppText(
                            text = stringResource(Res.string.operating_notice_closing_time, time.toString()),
                            style = AppTextStyle.B3,
                            color = GrayColor.C300,
                        )
                    }
                }
            }
        }
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            tint = GrayColor.C200,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OngoingOperatingNoticeShopPreview() {
    RamapTheme {
        Column {
            OperatingNoticePreviewParameterProvider().values.first().forEach { notice ->
                OngoingOperatingNoticeShop(
                    notice = notice,
                    onClick = {},
                )
            }
        }
    }
}
