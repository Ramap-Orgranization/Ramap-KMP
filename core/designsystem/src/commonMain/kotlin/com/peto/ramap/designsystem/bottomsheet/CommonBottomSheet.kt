package com.peto.ramap.designsystem.bottomsheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CommonBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    isBackEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    config: CommonBottomSheetConfig = CommonBottomSheetConfig(),
    content: @Composable ColumnScope.(Modifier) -> Unit,
) {
    if (!visible) return

    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var internalVisible by remember { mutableStateOf(false) }
    var isRendered by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            if (isImeVisible) focusManager.clearFocus()
            isRendered = true
            withFrameNanos { }
            internalVisible = true
        } else {
            internalVisible = false
            delay(EXIT_ANIMATION_DURATION_MILLIS.toLong().milliseconds)
            isRendered = false
        }
    }

    if (!isRendered) return

    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )
    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = isBackEnabled && internalVisible,
        onBackCompleted = onDismissRequest,
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().then(modifier),
    ) {
        val sheetMaxHeight = config.maxHeight ?: maxHeight * config.maxHeightFraction

        BottomSheetScrim(
            config = config,
            onDismissRequest = onDismissRequest,
        )
        BottomSheetContent(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = internalVisible,
            config = config,
            sheetMaxHeight = sheetMaxHeight,
            onDismissRequest = onDismissRequest,
            content = content,
        )
    }
}

@Composable
private fun BottomSheetScrim(
    config: CommonBottomSheetConfig,
    onDismissRequest: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(config.scrimColor)
                .clickable(
                    enabled = config.dismissOnScrimClick,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismissRequest,
                ),
    )
}

@Composable
private fun BottomSheetContent(
    modifier: Modifier,
    visible: Boolean,
    config: CommonBottomSheetConfig,
    sheetMaxHeight: Dp,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.(Modifier) -> Unit,
) {
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val dragModifier =
        if (config.isDraggable) {
            Modifier.draggable(
                orientation = Orientation.Vertical,
                state =
                    rememberDraggableState { dragAmount ->
                        dragOffsetPx = (dragOffsetPx + dragAmount).coerceAtLeast(0f)
                    },
                onDragStopped = {
                    if (dragOffsetPx >= with(density) { DRAG_DISMISS_THRESHOLD.toPx() }) {
                        onDismissRequest()
                    } else {
                        dragOffsetPx = 0f
                    }
                },
            )
        } else {
            Modifier
        }

    AnimatedVisibility(
        visible = visible,
        enter =
            slideInVertically(
                animationSpec = tween(SHEET_ENTER_DURATION_MILLIS),
                initialOffsetY = { it },
            ),
        exit =
            slideOutVertically(
                animationSpec = tween(EXIT_ANIMATION_DURATION_MILLIS),
                targetOffsetY = { it },
            ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = config.shape,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            color = CommonColor.White,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .graphicsLayer { translationY = dragOffsetPx },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (config.showHandle) {
                    SheetHandle(
                        config = config,
                        dragModifier = dragModifier,
                    )
                } else {
                    Spacer(Modifier.height(config.handleTopPadding + config.handleBottomPadding))
                }
                content(dragModifier)
            }
        }
    }
}

@Composable
private fun SheetHandle(
    config: CommonBottomSheetConfig,
    dragModifier: Modifier,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(config.handleTopPadding + 4.dp + config.handleBottomPadding)
                .then(dragModifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        color = GrayColor.C100,
                        shape = RoundedCornerShape(2.dp),
                    ),
        )
    }
}

private const val SHEET_ENTER_DURATION_MILLIS = 220
private const val EXIT_ANIMATION_DURATION_MILLIS = 180
private val DRAG_DISMISS_THRESHOLD = 120.dp
