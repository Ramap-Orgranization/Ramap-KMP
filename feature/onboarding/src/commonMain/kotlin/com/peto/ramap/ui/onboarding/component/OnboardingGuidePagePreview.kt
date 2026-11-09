package com.peto.ramap.ui.onboarding.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LocalAppTypography
import com.peto.ramap.ui.onboarding.model.OnboardingGuidePage
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val widestGuideAspectRatio = OnboardingGuidePage.entries.maxOf { it.sourceBounds.width / it.sourceBounds.height }

@Composable
internal fun OnboardingGuidePagePreview(
    page: OnboardingGuidePage,
    modifier: Modifier = Modifier,
) {
    val isNotificationMenu = page == OnboardingGuidePage.NotificationMenu
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GuideSpeechBubble(
            message = stringResource(page.message),
            singleLine = isNotificationMenu,
        )
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val aspectRatio = page.sourceBounds.width / page.sourceBounds.height
            val previewHeight = minOf(maxHeight, maxWidth / widestGuideAspectRatio)
            val imageWidth = maxOf(previewHeight * aspectRatio, minOf(maxWidth, 280.dp))
            val previewShape = RoundedCornerShape(16.dp)
            key(page) {
                Column(
                    modifier =
                        Modifier
                            .width(imageWidth)
                            .height(previewHeight)
                            .border(width = 1.dp, color = GrayColor.C200, shape = previewShape)
                            .clip(previewShape)
                            .verticalScroll(
                                state = rememberScrollState(),
                                reverseScrolling = isNotificationMenu,
                            ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GuideScreenshot(
                        page = page,
                        modifier = Modifier.width(imageWidth).aspectRatio(aspectRatio),
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideSpeechBubble(
    message: String,
    singleLine: Boolean,
) {
    val twoLineHeight = LocalAppTypography.current.t2.lineHeight.value.dp * 2
    val verticalPadding = 14.dp
    val pointerHeight = 9.dp
    Column(
        modifier = Modifier.heightIn(min = twoLineHeight + verticalPadding * 2 + pointerHeight),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(
            text = message,
            style = if (singleLine) AppTextStyle.T3 else AppTextStyle.T2,
            color = CommonColor.White,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .background(GrayColor.C500, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = verticalPadding)
                    .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Canvas(modifier = Modifier.size(width = 18.dp, height = pointerHeight)) {
            val pointer =
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
            drawPath(path = pointer, color = GrayColor.C500)
        }
    }
}

@Composable
private fun GuideScreenshot(
    page: OnboardingGuidePage,
    modifier: Modifier = Modifier,
) {
    val screenshot = imageResource(page.image)
    val description = stringResource(page.imageDescription)
    val source = page.sourceBounds
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        drawImage(
            image = screenshot,
            srcOffset = IntOffset(source.left.roundToInt(), source.top.roundToInt()),
            srcSize = IntSize(source.width.roundToInt(), source.height.roundToInt()),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
        val scale = size.width / source.width
        val targets =
            page.highlights.map { target ->
                Rect(
                    left = (target.left - source.left) * scale,
                    top = (target.top - source.top) * scale,
                    right = (target.right - source.left) * scale,
                    bottom = (target.bottom - source.top) * scale,
                )
            }
        val cornerRadius = CornerRadius(10.dp.toPx())
        val dimmedArea =
            Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                targets.forEach { addRoundRect(RoundRect(it, cornerRadius)) }
            }
        drawPath(path = dimmedArea, color = CommonColor.Black.copy(alpha = 0.18f))
        targets.forEach { target ->
            drawRoundRect(
                color = ChromaticColor.Orange400,
                topLeft = target.topLeft,
                size = target.size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }
    }
}
