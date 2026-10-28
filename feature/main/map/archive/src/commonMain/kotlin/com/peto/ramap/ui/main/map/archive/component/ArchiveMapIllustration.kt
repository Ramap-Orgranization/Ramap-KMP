package com.peto.ramap.ui.main.map.archive.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.peto.ramap.ui.main.map.archive.model.ArchiveMarkerPainter
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ill_archive_loading_map
import ramap.shared.generated.resources.img_archive_loading_photo_a
import ramap.shared.generated.resources.img_archive_loading_photo_b
import ramap.shared.generated.resources.marker_visited_check
import ramap.shared.generated.resources.marker_visited_photo_frame

private val MarkerLandingEasing = CubicBezierEasing(0.22f, 0.7f, 0.36f, 1f)

@Composable
internal fun ArchiveMapIllustration(transition: InfiniteTransition? = null) {
    val frame = painterResource(Res.drawable.marker_visited_photo_frame)
    val firstPhoto = painterResource(Res.drawable.img_archive_loading_photo_a)
    val secondPhoto = painterResource(Res.drawable.img_archive_loading_photo_b)
    val firstMarker = remember(frame, firstPhoto) { ArchiveMarkerPainter(shape = frame, photo = firstPhoto) }
    val secondMarker = remember(frame, secondPhoto) { ArchiveMarkerPainter(shape = frame, photo = secondPhoto) }
    BoxWithConstraints(
        modifier =
            Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .aspectRatio(1.4f),
    ) {
        val illustrationScale = maxWidth / 280.dp
        Image(
            painter = painterResource(Res.drawable.ill_archive_loading_map),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        ArchiveLoadingMarker(
            painter = firstMarker,
            transition = transition,
            startDelayMillis = 0,
            illustrationScale = illustrationScale,
            modifier =
                Modifier
                    .absoluteOffset(x = 29.dp * illustrationScale, y = 54.dp * illustrationScale)
                    .size(width = 72.dp * illustrationScale, height = 82.dp * illustrationScale),
        )
        ArchiveLoadingMarker(
            painter = secondMarker,
            transition = transition,
            startDelayMillis = 300,
            illustrationScale = illustrationScale,
            modifier =
                Modifier
                    .absoluteOffset(x = 182.dp * illustrationScale, y = 21.dp * illustrationScale)
                    .size(width = 72.dp * illustrationScale, height = 82.dp * illustrationScale),
        )
        ArchiveLoadingMarker(
            painter = painterResource(Res.drawable.marker_visited_check),
            transition = transition,
            startDelayMillis = 600,
            illustrationScale = illustrationScale,
            modifier =
                Modifier
                    .absoluteOffset(x = 164.dp * illustrationScale, y = 133.dp * illustrationScale)
                    .size(width = 34.dp * illustrationScale, height = 42.dp * illustrationScale),
        )
    }
}

@Composable
private fun ArchiveLoadingMarker(
    painter: Painter,
    transition: InfiniteTransition?,
    startDelayMillis: Int,
    illustrationScale: Float,
    modifier: Modifier = Modifier,
) {
    if (transition == null) {
        Image(painter = painter, contentDescription = null, modifier = modifier)
        return
    }
    val progress by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = 3800
                        0f at 0
                        0f at 266 using MarkerLandingEasing
                        1f at 836
                        1f at 3230 using MarkerLandingEasing
                        0f at 3800
                    },
                initialStartOffset = StartOffset(startDelayMillis),
            ),
        label = "archive_loading_marker_$startDelayMillis",
    )
    Image(
        painter = painter,
        contentDescription = null,
        modifier =
            modifier.graphicsLayer {
                alpha = 0.45f + 0.55f * progress
                translationY = -9.dp.toPx() * illustrationScale * (1f - progress)
                scaleX = 0.92f + 0.08f * progress
                scaleY = scaleX
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.9f)
            },
    )
}
