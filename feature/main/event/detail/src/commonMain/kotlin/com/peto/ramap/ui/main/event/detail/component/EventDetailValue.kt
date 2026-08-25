package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
fun EventDetailValue(
    value: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) = AppText(
    value,
    modifier = modifier,
    style = AppTextStyle.B2,
    color = GrayColor.C500,
    textAlign = textAlign,
)

@Preview(showBackground = true)
@Composable
private fun EventDetailValuePreview() {
    RamapTheme {
        EventDetailValue(
            value = "이벤트 상세 설명 내용입니다. 여러 줄의 텍스트가 들어갈 수 있습니다.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
