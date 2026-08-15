package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun ShopInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClickLabel: String? = null,
    showBusinessHoursNotice: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = label,
                style = AppTextStyle.B1,
                color = GrayColor.C300,
            )
            if (showBusinessHoursNotice) {
                BusinessHoursNotice()
            }
        }
        AppText(
            text = value,
            modifier =
                Modifier
                    .weight(1f)
                    .noRippleClickable(
                        onClick = onClick,
                        onClickLabel = onClickLabel,
                    ),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopInfoRowPreview() {
    RamapTheme {
        ShopInfoRow(
            label = "주소",
            value = "서울 강남구 테헤란로 123",
            onClick = {},
        )
    }
}
