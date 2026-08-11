package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun EventTag(text: String) {
    AppText(
        text,
        modifier =
            Modifier
                .background(ChromaticColor.Yellow400, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        style = AppTextStyle.T3,
        color = GrayColor.C500,
    )
}

@Preview
@Composable
private fun EventTagPreview() {
    RamapTheme {
        EventTag(text = "팝업스토어")
    }
}
