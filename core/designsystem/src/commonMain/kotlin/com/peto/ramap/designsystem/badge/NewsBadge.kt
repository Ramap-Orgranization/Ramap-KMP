package com.peto.ramap.designsystem.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.operatingnotice.OperatingNoticeResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.stringResource

@Composable
fun NewsBadge(
    text: String,
    textStyle: AppTextStyle,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    AppText(
        text = text,
        modifier =
            modifier
                .background(
                    containerColor,
                    RoundedCornerShape(999.dp),
                ).padding(horizontal = 8.dp, vertical = 3.dp),
        style = textStyle,
        color = contentColor,
    )
}

@Preview(showBackground = true)
@Composable
private fun NewsBadgePreview() {
    RamapTheme {
        NewsBadge(
            text = "영업중",
            textStyle = AppTextStyle.C2,
            containerColor = SystemColor.Warning,
            contentColor = CommonColor.White,
        )
    }
}

@Preview(name = "영업 변동 유형별 색상", showBackground = true, widthDp = 360)
@Composable
private fun OperatingNoticeBadgeColorsPreview() {
    RamapTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OperatingNoticeType.entries.forEach { type ->
                NewsBadge(
                    text = stringResource(OperatingNoticeResourceMapper.typeLabel(type)),
                    textStyle = AppTextStyle.C2,
                    containerColor = OperatingNoticeResourceMapper.badgeContainerColor(type),
                    contentColor = CommonColor.White,
                )
            }
        }
    }
}
