package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor

@Composable
fun ReviewPixelAvatar(
    seed: String,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        drawRect(ChromaticColor.Purple400)
        val cell = size.minDimension / 9f
        val hash = seed.hashCode()
        for (row in 0..4) {
            for (column in 0..2) {
                if ((hash ushr (row * 3 + column)) and 1 == 1 || row == 2) {
                    drawRect(GrayColor.C500, Offset((column + 2) * cell, (row + 2) * cell), Size(cell, cell))
                    drawRect(GrayColor.C500, Offset((6 - column) * cell, (row + 2) * cell), Size(cell, cell))
                }
            }
        }
        drawRect(Color.White, Offset(3 * cell, 4 * cell), Size(cell, cell))
        drawRect(Color.White, Offset(5 * cell, 4 * cell), Size(cell, cell))
    }
}
