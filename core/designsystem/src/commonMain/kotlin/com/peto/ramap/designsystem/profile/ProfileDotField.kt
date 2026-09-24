package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.RamapTheme

@Composable
fun ProfileDotField(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val step = 12.dp.toPx()
        val rowCount = (size.height / step).toInt()
        val columnCount = (size.width / step).toInt()

        for (row in 0..rowCount) {
            val alpha = (1f - row * step / size.height).coerceIn(0f, 1f) * 0.6f
            for (column in 0..columnCount) {
                drawCircle(
                    color = Color(0xFFA9AD97).copy(alpha = alpha),
                    radius = 1.dp.toPx(),
                    center =
                        Offset(
                            x = column * step,
                            y = row * step,
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileDotFieldPreview() {
    RamapTheme {
        ProfileDotField(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
        )
    }
}
