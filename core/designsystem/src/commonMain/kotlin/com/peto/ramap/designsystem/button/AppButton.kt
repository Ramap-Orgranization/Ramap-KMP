package com.peto.ramap.designsystem.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@Composable
fun AppButton(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = CommonColor.White,
    textStyle: AppTextStyle = AppTextStyle.T2,
    backgroundColor: Color = GrayColor.C500,
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    border: BorderStroke? = null,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.height(52.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius),
        border = border,
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = text,
                color = textColor,
                style = textStyle,
                textAlign = TextAlign.Center,
            )
        }
    }
}
