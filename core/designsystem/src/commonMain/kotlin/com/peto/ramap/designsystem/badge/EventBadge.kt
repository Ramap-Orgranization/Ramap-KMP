package com.peto.ramap.designsystem.badge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EventBadge(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: AppTextStyle = AppTextStyle.B3,
) {
    NewsBadge(
        text = text,
        textStyle = textStyle,
        containerColor = SystemColor.Warning,
        contentColor = CommonColor.White,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun EventBadgePreview() {
    RamapTheme {
        EventBadge(text = "영업 변동")
    }
}
