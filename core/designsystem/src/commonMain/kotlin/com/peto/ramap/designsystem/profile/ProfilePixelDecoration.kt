package com.peto.ramap.designsystem.profile

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun ProfileCheckStamp(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawCircle(Color(0xFFFFFEFB))
        drawCircle(Color(0xFFE95432), size.minDimension * 0.39f)
        val path =
            Path().apply {
                moveTo(size.width * 0.32f, size.height * 0.5f)
                lineTo(size.width * 0.46f, size.height * 0.64f)
                lineTo(size.width * 0.69f, size.height * 0.37f)
            }
        drawPath(path, Color.White, style = Stroke(size.width * 0.055f))
    }
}

@Composable
fun ProfileFooterPixels(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cell = size.height / 3f
        for (row in 0..2) {
            for (column in 0..6) {
                if ((row + column) % 2 == 0) {
                    drawRect(Color(0xFF959985).copy(alpha = 0.55f), Offset(column * cell, row * cell), Size(cell, cell))
                }
            }
        }
    }
}
