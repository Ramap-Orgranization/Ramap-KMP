package com.peto.ramap.ui.shop.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberShopDetailHandleDragState(
    bottomSheetState: SheetState,
    onDismissRequest: () -> Unit,
): ShopDetailHandleDragState {
    val density = LocalDensity.current
    var dragDistancePx by remember { mutableFloatStateOf(0f) }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val modifier =
        Modifier.draggable(
            orientation = Orientation.Vertical,
            enabled = bottomSheetState.currentValue == SheetValue.PartiallyExpanded,
            state =
                rememberDraggableState { dragAmount ->
                    dragDistancePx += dragAmount
                    offsetPx = (offsetPx + dragAmount).coerceAtLeast(0f)
                },
            onDragStarted = {
                dragDistancePx = 0f
                offsetPx = 0f
            },
            onDragStopped = {
                if (offsetPx >= with(density) { DRAG_DISMISS_THRESHOLD.toPx() }) {
                    onDismissRequest()
                } else {
                    offsetPx = 0f
                    if (dragDistancePx <= -with(density) { DRAG_EXPAND_THRESHOLD.toPx() }) {
                        bottomSheetState.expand()
                    }
                }
                dragDistancePx = 0f
            },
        )

    return remember(modifier, offsetPx) {
        ShopDetailHandleDragState(
            modifier = modifier,
            offsetPx = offsetPx,
        )
    }
}

internal data class ShopDetailHandleDragState(
    val modifier: Modifier,
    val offsetPx: Float,
)

private val DRAG_DISMISS_THRESHOLD = 120.dp
private val DRAG_EXPAND_THRESHOLD = 40.dp
