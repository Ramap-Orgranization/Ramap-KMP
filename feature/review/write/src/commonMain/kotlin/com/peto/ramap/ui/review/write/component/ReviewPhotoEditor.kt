package com.peto.ramap.ui.review.write.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.peto.ramap.designsystem.image.LocalImageAccessScope
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.ReviewAttachment
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_camera_add
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_photo_library
import ramap.shared.generated.resources.ic_photo_slot_frame_dashed
import ramap.shared.generated.resources.review_photo_description
import ramap.shared.generated.resources.review_photos
import ramap.shared.generated.resources.shop_review_image_add
import ramap.shared.generated.resources.shop_review_image_change
import ramap.shared.generated.resources.shop_review_image_count
import ramap.shared.generated.resources.shop_review_image_remove
import kotlin.math.roundToInt

@Composable
internal fun PhotoEditor(
    state: ReviewWriteUiState,
    onPick: () -> Unit,
    dispatch: (ReviewWriteIntent) -> Unit,
) {
    val scrollState = rememberScrollState()
    val onAddPhoto = onPick.takeIf { state.canEdit }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = stringResource(Res.string.review_photos),
                style = AppTextStyle.T2,
                color = GrayColor.C500,
            )
            Spacer(modifier = Modifier.weight(1f))
            AppText(
                text =
                    stringResource(
                        Res.string.shop_review_image_count,
                        state.imageCount,
                        ReviewImage.MAX_COUNT,
                    ),
                style = AppTextStyle.C1,
                color = GrayColor.C500,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(PHOTO_SLOT_SPACING),
        ) {
            repeat(ReviewImage.MAX_COUNT) { index ->
                val attachment = state.attachments.getOrNull(index)
                if (attachment == null) {
                    AddPhotoSlot(
                        showLabel = index == state.imageCount,
                        onPick = onAddPhoto,
                    )
                } else {
                    AttachedPhotoSlot(
                        attachment = attachment,
                        index = index,
                        imageCount = state.imageCount,
                        canEdit = state.canEdit,
                        scrollState = scrollState,
                        onMove = { dispatch(ReviewWriteIntent.MoveImage(index, it)) },
                        onRemove = { dispatch(ReviewWriteIntent.RemoveImage(index)) },
                    )
                }
            }
        }
        AppText(
            text = stringResource(Res.string.shop_review_image_change),
            style = AppTextStyle.C1,
            color = GrayColor.C300,
        )
    }
}

@Composable
private fun AddPhotoSlot(
    showLabel: Boolean,
    onPick: (() -> Unit)?,
) {
    val label = stringResource(Res.string.shop_review_image_add)
    DashedPhotoSlot(
        modifier = Modifier.size(PHOTO_SLOT_SIZE).semantics { role = Role.Button },
        onClick = onPick,
    ) {
        if (showLabel) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_camera_add),
                    contentDescription = null,
                    tint = GrayColor.C500,
                )
                AppText(
                    text = label,
                    style = AppTextStyle.C1,
                    color = GrayColor.C500,
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_photo_library),
                    contentDescription = label,
                    tint = GrayColor.C300,
                )
            }
        }
    }
}

@Composable
private fun DashedPhotoSlot(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        color = GrayColor.C050,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            propagateMinConstraints = true,
        ) {
            content()

            Image(
                painter =
                    painterResource(
                        Res.drawable.ic_photo_slot_frame_dashed,
                    ),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(GrayColor.C300),
            )
        }
    }
}

@Composable
private fun AttachedPhotoSlot(
    attachment: ReviewAttachment,
    index: Int,
    imageCount: Int,
    canEdit: Boolean,
    scrollState: ScrollState,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current
    val slotWidthPx = with(density) { PHOTO_SLOT_SIZE.roundToPx() }
    val slotPitchPx = slotWidthPx + with(density) { PHOTO_SLOT_SPACING.roundToPx() }
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val canMove = canEdit && imageCount > 1
    var dragOffsetPx by remember(attachment, index, canMove, imageCount) { mutableStateOf(0f) }
    var dragStartScrollPx by remember(attachment, index, canMove, imageCount) { mutableStateOf(0) }
    var dragStartPointerPx by remember(attachment, index, canMove, imageCount) { mutableStateOf<Float?>(null) }
    val isDragging = dragStartPointerPx != null

    LaunchedEffect(dragStartPointerPx, slotPitchPx, imageCount, canMove) {
        val start = dragStartPointerPx ?: return@LaunchedEffect
        val edgePx = with(density) { DRAG_SCROLL_EDGE.toPx() }
        val speedPxPerSecond = with(density) { DRAG_SCROLL_SPEED.toPx() }
        var previousFrame = withFrameNanos { it }
        while (isActive) {
            val frame = withFrameNanos { it }
            val elapsedSeconds = (frame - previousFrame) / NANOS_PER_SECOND
            previousFrame = frame
            val viewportPx = scrollState.viewportSize
            val maxScrollPx = ((imageCount - 1) * slotPitchPx + slotWidthPx - viewportPx).coerceAtLeast(0)
            if (dragOffsetPx == 0f) continue
            val pointerPx = start + dragOffsetPx
            val distancePx =
                when {
                    pointerPx < edgePx -> -minOf(edgePx - pointerPx, edgePx)
                    pointerPx > viewportPx - edgePx -> minOf(pointerPx - viewportPx + edgePx, edgePx)
                    else -> 0f
                }
            val step = distancePx / edgePx * speedPxPerSecond * elapsedSeconds
            val scrollStep =
                if (step > 0) {
                    step.coerceAtMost((maxScrollPx - scrollState.value).coerceAtLeast(0).toFloat())
                } else {
                    step
                }
            if (scrollStep != 0f) scrollState.scrollBy(scrollStep)
        }
    }

    Box(
        modifier =
            Modifier
                .size(PHOTO_SLOT_SIZE)
                .zIndex(if (isDragging) 1f else 0f)
                .pointerInput(attachment, index, imageCount, canMove, slotPitchPx, direction) {
                    if (canMove) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { start ->
                                dragStartScrollPx = scrollState.value
                                dragOffsetPx = 0f
                                val pointerInSlotPx = if (direction == 1) start.x else slotWidthPx - start.x
                                dragStartPointerPx = index * slotPitchPx - dragStartScrollPx + pointerInSlotPx
                            },
                            onDragCancel = {
                                dragStartPointerPx = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetPx += direction * dragAmount.x
                            },
                            onDragEnd = {
                                val displacementPx = dragOffsetPx + scrollState.value - dragStartScrollPx
                                val targetIndex = (index + (displacementPx / slotPitchPx).roundToInt()).coerceIn(0, imageCount - 1)
                                dragStartPointerPx = null
                                if (targetIndex != index) onMove(targetIndex)
                            },
                        )
                    }
                }.graphicsLayer {
                    translationX =
                        if (isDragging) {
                            direction * (dragOffsetPx + scrollState.value - dragStartScrollPx)
                        } else {
                            0f
                        }
                },
    ) {
        key(LocalImageAccessScope.current) {
            AsyncImage(
                model =
                    when (attachment) {
                        is ReviewAttachment.Existing -> attachment.url
                        is ReviewAttachment.New -> attachment.image.bytes
                    },
                contentDescription = stringResource(Res.string.review_photo_description, index + 1),
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            shape = CircleShape,
            color = CommonColor.Black.copy(alpha = 0.55f),
        ) {
            IconButton(
                onClick = onRemove,
                enabled = canEdit,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = stringResource(Res.string.shop_review_image_remove),
                    tint = CommonColor.White,
                )
            }
        }
    }
}

private val PHOTO_SLOT_SIZE = 112.dp
private val PHOTO_SLOT_SPACING = 8.dp
private val DRAG_SCROLL_EDGE = 36.dp
private val DRAG_SCROLL_SPEED = 360.dp
private const val NANOS_PER_SECOND = 1_000_000_000f

@Preview(showBackground = true)
@Composable
private fun ReviewPhotoEditorPreview() {
    RamapTheme {
        PhotoEditor(
            state = ReviewWriteUiState(),
            onPick = {},
            dispatch = {},
        )
    }
}
