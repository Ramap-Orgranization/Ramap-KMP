package com.peto.ramap.ui.main.map.archive.component

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.visited_shops_loading_description
import ramap.shared.generated.resources.visited_shops_loading_title

@Composable
internal fun ArchiveMapLoadingContent(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "archive_map_loading")
    BoxWithConstraints(
        modifier =
            modifier
                .background(CommonColor.White)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Final).changes.forEach { it.consume() }
                        }
                    }
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 22.dp)
                    .padding(bottom = if (maxHeight < 480.dp) 24.dp else 70.dp)
                    .semantics(mergeDescendants = true) {
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        liveRegion = LiveRegionMode.Polite
                    },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ArchiveMapIllustration(transition = transition)
            Spacer(modifier = Modifier.height(25.dp))
            AppText(
                text = stringResource(Res.string.visited_shops_loading_title),
                style = AppTextStyle.H3Brand,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppText(
                text = stringResource(Res.string.visited_shops_loading_description),
                style = AppTextStyle.B2,
                color = GrayColor.C300,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(23.dp))
            ArchiveLoadingDots(transition = transition)
        }
    }
}

@Composable
private fun ArchiveLoadingDots(transition: InfiniteTransition) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val progress by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            keyframes {
                                durationMillis = 3800
                                0f at 0
                                1f at 950
                                0f at 2470
                                0f at 3800
                            },
                        initialStartOffset = StartOffset(index * 160),
                    ),
                label = "archive_loading_dot_$index",
            )
            Box(
                modifier =
                    Modifier
                        .size(5.dp)
                        .graphicsLayer {
                            alpha = 0.3f + 0.7f * progress
                            translationY = -2.dp.toPx() * progress
                        }.background(color = ChromaticColor.Orange400, shape = CircleShape),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun ArchiveMapLoadingContentPreview() {
    RamapTheme {
        ArchiveMapLoadingContent(modifier = Modifier.fillMaxSize())
    }
}
