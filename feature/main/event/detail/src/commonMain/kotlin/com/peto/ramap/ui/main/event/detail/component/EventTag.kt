package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor

@Composable
internal fun EventTag(
    text: String,
    isCancelledToday: Boolean = false,
) {
    AppText(
        text,
        modifier =
            Modifier
                .background(
                    if (isCancelledToday) SystemColor.Warning else ChromaticColor.Yellow400,
                    RoundedCornerShape(999.dp),
                ).padding(horizontal = 10.dp, vertical = 4.dp),
        style = AppTextStyle.T3,
        color = if (isCancelledToday) CommonColor.White else GrayColor.C500,
    )
}

@Preview
@Composable
private fun EventTagPreview() {
    RamapTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EventTag(text = "팝업스토어")

            EventTag(text = "콜라보", isCancelledToday = true)
        }
    }
}
