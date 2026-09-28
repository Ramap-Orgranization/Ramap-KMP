package com.peto.ramap.ui.review.write.component

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_camera_add
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_photo_library
import ramap.shared.generated.resources.review_photo_description
import ramap.shared.generated.resources.review_photos
import ramap.shared.generated.resources.shop_review_image_add
import ramap.shared.generated.resources.shop_review_image_count
import ramap.shared.generated.resources.shop_review_image_limit
import ramap.shared.generated.resources.shop_review_image_remove

@Composable
internal fun PhotoEditor(
    state: ReviewWriteUiState,
    onPick: () -> Unit,
    dispatch: (ReviewWriteIntent) -> Unit,
) {
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
                text = stringResource(Res.string.shop_review_image_count, state.images.size, ReviewImage.MAX_COUNT),
                style = AppTextStyle.C1,
                color = GrayColor.C500,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val slotPitchPx = with(LocalDensity.current) { (PHOTO_SLOT_SIZE + PHOTO_SLOT_SPACING).toPx() }

            repeat(ReviewImage.MAX_COUNT) { index ->
                val image = state.images.getOrNull(index)
                if (image == null) {
                    if (index == state.images.size) {
                        PhotoSlot(
                            image = null,
                            index = null,
                            size = PHOTO_SLOT_SIZE,
                            onPick = onPick,
                            isPickEnabled = !state.isSubmitting,
                        )
                    } else {
                        EmptyPhotoSlot(PHOTO_SLOT_SIZE)
                    }
                } else {
                    PhotoSlot(
                        image = image,
                        index = index,
                        size = PHOTO_SLOT_SIZE,
                        slotPitchPx = slotPitchPx,
                        imageCount = state.images.size,
                        isSubmitting = state.isSubmitting,
                        onPick = null,
                        onMove = { fromIndex, toIndex ->
                            dispatch(ReviewWriteIntent.MoveImage(fromIndex, toIndex))
                        },
                        onRemove = { dispatch(ReviewWriteIntent.RemoveImage(index)) },
                    )
                }
            }
        }
        AppText(
            text = stringResource(Res.string.shop_review_image_limit),
            style = AppTextStyle.C1,
            color = GrayColor.C300,
        )
    }
}

@Composable
private fun EmptyPhotoSlot(size: Dp) {
    DashedPhotoSlot(modifier = Modifier.size(size)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.ic_photo_library),
                contentDescription = null,
                tint = GrayColor.C300,
            )
        }
    }
}

@Composable
private fun DashedPhotoSlot(
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cornerRadius = 12.dp
    Surface(
        modifier =
            modifier.drawWithContent {
                drawContent()
                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    color = GrayColor.C300,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            },
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(cornerRadius),
        color = GrayColor.C050,
    ) {
        content()
    }
}

@Composable
private fun PhotoSlot(
    image: ReviewImage?,
    index: Int?,
    size: Dp,
    slotPitchPx: Float = 0f,
    imageCount: Int = 0,
    isSubmitting: Boolean = false,
    onPick: (() -> Unit)?,
    isPickEnabled: Boolean = false,
    onMove: ((Int, Int) -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    if (image == null) {
        DashedPhotoSlot(
            modifier = Modifier.size(size),
            onClick = onPick?.takeIf { isPickEnabled },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_camera_add),
                    contentDescription = stringResource(Res.string.shop_review_image_add),
                    tint = GrayColor.C500,
                )
                AppText(
                    text = stringResource(Res.string.shop_review_image_add),
                    style = AppTextStyle.C1,
                    color = GrayColor.C500,
                )
            }
        }
        return
    }

    val imageIndex = requireNotNull(index)
    var dragOffsetX by remember(index, image) { mutableStateOf(0f) }
    var isDragging by remember(index, image) { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .size(size)
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer { translationX = dragOffsetX }
                .pointerInput(imageIndex, imageCount, isSubmitting, slotPitchPx) {
                    if (!isSubmitting) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { isDragging = true },
                            onDragCancel = {
                                dragOffsetX = 0f
                                isDragging = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX += dragAmount.x
                            },
                            onDragEnd = {
                                val indexDelta =
                                    if (dragOffsetX >= 0f) {
                                        ((dragOffsetX + slotPitchPx / 2) / slotPitchPx).toInt()
                                    } else {
                                        ((dragOffsetX - slotPitchPx / 2) / slotPitchPx).toInt()
                                    }
                                val targetIndex = (imageIndex + indexDelta).coerceIn(0, imageCount - 1)
                                dragOffsetX = 0f
                                isDragging = false
                                if (targetIndex != imageIndex) onMove?.invoke(imageIndex, targetIndex)
                            },
                        )
                    }
                },
    ) {
        AsyncImage(
            model = image.bytes,
            contentDescription = stringResource(Res.string.review_photo_description, imageIndex + 1),
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            shape = CircleShape,
            color = CommonColor.Black.copy(alpha = 0.55f),
        ) {
            IconButton(
                onClick = { onRemove?.invoke() },
                enabled = !isSubmitting,
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
