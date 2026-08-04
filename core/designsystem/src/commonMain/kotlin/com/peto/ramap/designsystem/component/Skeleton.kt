package com.peto.ramap.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.GrayColor

fun Modifier.shimmer(
    visible: Boolean = true,
    shape: Shape = RoundedCornerShape(4.dp),
    baseColor: Color = GrayColor.C200,
    highlightColor: Color = GrayColor.C100,
): Modifier =
    composed {
        if (!visible) return@composed this

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmer_translate",
        )

        val shimmerColors =
            listOf(
                baseColor,
                highlightColor,
                baseColor,
            )

        val brush =
            Brush.linearGradient(
                colors = shimmerColors,
                start = Offset.Zero,
                end = Offset(x = translateAnim, y = translateAnim),
            )

        background(brush = brush, shape = shape)
    }

@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier = modifier.shimmer(shape = shape),
    )
}
