package com.peto.ramap.designsystem.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppRoundButton(
    contentHeight: Dp,
    contentColor: Color,
    contentBorderColor: Color,
    contentBorderWidth: Dp,
    shadowHeight: Dp,
    shadowOffset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)

    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(shadowHeight)
                    .offset(y = shadowOffset)
                    .background(color = contentBorderColor, shape = shape),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
                    .background(color = contentColor, shape = shape)
                    .border(
                        color = contentBorderColor,
                        shape = shape,
                        width = contentBorderWidth,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
