package com.peto.ramap.designsystem.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun NewsBadge(
    text: String,
    textStyle: AppTextStyle,
    modifier: Modifier = Modifier,
    containerColor: Color = ChromaticColor.Yellow400,
    contentColor: Color = GrayColor.C500,
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
