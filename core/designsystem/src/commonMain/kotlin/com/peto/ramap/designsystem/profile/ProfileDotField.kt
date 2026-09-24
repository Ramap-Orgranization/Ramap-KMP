package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProfileDotField(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val step = 12.dp.toPx()
        for (row in 0..(size.height / step).toInt()) {
            val alpha = (1f - row * step / size.height).coerceIn(0f, 1f) * 0.6f
            for (column in 0..(size.width / step).toInt()) {
                drawCircle(Color(0xFFA9AD97).copy(alpha = alpha), 1.dp.toPx(), Offset(column * step, row * step))
            }
        }
    }
}
