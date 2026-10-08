package com.peto.ramap.ui.main.map.shop

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.DimmedColor
import com.peto.ramap.theme.GrayColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailBottomSheet(
    shopId: String,
    maxHeight: Dp,
    isBackEnabled: Boolean,
    isNavigationBarPadded: Boolean,
    onDismissRequest: () -> Unit,
    floatingAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(ScrollState) -> Unit,
) {
    key(shopId) {
        val scrollState = rememberScrollState()
        val bottomSheetState =
            rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
            )
        val scaffoldState =
            rememberBottomSheetScaffoldState(
                bottomSheetState = bottomSheetState,
            )
        val backEventState =
            rememberNavigationEventState<NavigationEventInfo>(
                currentInfo = NavigationEventInfo.None,
            )
        val scrimInteractionSource = remember { MutableInteractionSource() }
        val density = LocalDensity.current
        var handleDragDistancePx by remember { mutableFloatStateOf(0f) }
        var handleDragOffsetPx by remember { mutableFloatStateOf(0f) }
        val handleDragModifier =
            Modifier.draggable(
                orientation = Orientation.Vertical,
                enabled = bottomSheetState.currentValue == SheetValue.PartiallyExpanded,
                state =
                    rememberDraggableState { dragAmount ->
                        handleDragDistancePx += dragAmount
                        handleDragOffsetPx = (handleDragOffsetPx + dragAmount).coerceAtLeast(0f)
                    },
                onDragStarted = {
                    handleDragDistancePx = 0f
                    handleDragOffsetPx = 0f
                },
                onDragStopped = {
                    if (handleDragOffsetPx >= with(density) { DRAG_DISMISS_THRESHOLD.toPx() }) {
                        onDismissRequest()
                    } else {
                        handleDragOffsetPx = 0f
                        if (handleDragDistancePx <= -with(density) { DRAG_EXPAND_THRESHOLD.toPx() }) {
                            bottomSheetState.expand()
                        }
                    }
                    handleDragDistancePx = 0f
                },
            )

        NavigationBackHandler(
            state = backEventState,
            isBackEnabled = isBackEnabled,
            onBackCompleted = onDismissRequest,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(DimmedColor.D020)
                        .clickable(
                            interactionSource = scrimInteractionSource,
                            indication = null,
                            onClick = onDismissRequest,
                        ),
            )

            BottomSheetScaffold(
                modifier =
                    Modifier
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                        ).graphicsLayer { translationY = handleDragOffsetPx },
                scaffoldState = scaffoldState,
                sheetPeekHeight = maxHeight * COLLAPSED_HEIGHT_FRACTION,
                sheetMaxWidth = Dp.Unspecified,
                sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                sheetContainerColor = CommonColor.White,
                sheetShadowElevation = 16.dp,
                containerColor = Color.Transparent,
                sheetDragHandle = null,
                sheetContent = {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(maxHeight)
                                .run {
                                    if (isNavigationBarPadded) navigationBarsPadding() else this
                                }.verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ShopDetailSheetHandle(dragModifier = handleDragModifier)
                        content(scrollState)
                    }
                },
                content = {},
            )

            val isFullyExpanded =
                bottomSheetState.currentValue == SheetValue.Expanded &&
                    bottomSheetState.targetValue == SheetValue.Expanded &&
                    !bottomSheetState.isAnimationRunning
            if (floatingAction != null && isFullyExpanded) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .run {
                                if (isNavigationBarPadded) navigationBarsPadding() else this
                            }.padding(bottom = 12.dp)
                            .graphicsLayer { translationY = handleDragOffsetPx },
                ) {
                    floatingAction()
                }
            }
        }
    }
}

@Composable
private fun ShopDetailSheetHandle(dragModifier: Modifier) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(dragModifier)
                .padding(top = 25.dp, bottom = 15.dp),
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

private const val COLLAPSED_HEIGHT_FRACTION = 0.5f
private val DRAG_DISMISS_THRESHOLD = 120.dp
private val DRAG_EXPAND_THRESHOLD = 40.dp
