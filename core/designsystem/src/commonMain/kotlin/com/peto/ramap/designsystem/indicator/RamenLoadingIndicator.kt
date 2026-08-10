package com.peto.ramap.designsystem.indicator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_progress

@Composable
fun RamenLoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ramen_loading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resource = Res.drawable.ic_progress),
            contentDescription = null,
            modifier =
                Modifier
                    .size(120.dp)
                    .graphicsLayer { rotationZ = rotation },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RamenLoadingIndicatorPreview() {
    RamenLoadingIndicator()
}
