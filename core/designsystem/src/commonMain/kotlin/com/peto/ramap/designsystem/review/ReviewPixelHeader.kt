package com.peto.ramap.designsystem.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor

@Composable
fun ReviewPixelHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    val step = 12.dp.toPx()
                    val dot = 1.dp.toPx()
                    for (row in 0..(size.height / step).toInt()) {
                        for (column in 0..(size.width / step).toInt()) {
                            drawRect(
                                color = colors.onSurface.copy(alpha = 0.12f),
                                topLeft = Offset(column * step, row * step),
                                size = Size(dot, dot),
                            )
                        }
                    }
                },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppText(
                text = title,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { heading() },
                style = AppTextStyle.H1,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ReviewPixelRamen(
                modifier = Modifier.size(72.dp),
            )
        }
        AppText(
            text = description,
            style = AppTextStyle.B3,
            color = colors.onSurfaceVariant,
        )
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp),
        ) {
            val cell = 4.dp.toPx()
            for (column in 0..(size.width / cell).toInt()) {
                drawRect(
                    color = colors.onSurface,
                    topLeft = Offset(column * cell, (column % 2) * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

@Composable
private fun ReviewPixelRamen(
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val pixels =
        listOf(
            "............",
            "...#..#.....",
            "..#..#......",
            "...#..#.....",
            "............",
            ".##########.",
            ".#oooooooo#.",
            "..#wwwwww#..",
            "..#wwwwww#..",
            "...#wwww#...",
            "....####....",
            "............",
        )
    Canvas(
        modifier = modifier.background(ChromaticColor.Green400),
    ) {
        val cell = size.minDimension / 12f
        for (row in pixels.indices) {
            for (column in pixels[row].indices) {
                val color =
                    when (pixels[row][column]) {
                        '#' -> ink
                        'w' -> Color.White
                        'o' -> ChromaticColor.Orange400
                        else -> continue
                    }
                drawRect(
                    color = color,
                    topLeft = Offset(column * cell, row * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}
