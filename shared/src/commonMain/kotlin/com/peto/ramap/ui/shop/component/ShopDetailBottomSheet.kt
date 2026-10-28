package com.peto.ramap.ui.shop.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.DimmedColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.paging.InfiniteLazyColumn
import com.peto.ramap.ui.paging.InfiniteScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailBottomSheet(
    shopId: String,
    listState: LazyListState,
    maxHeight: Dp,
    isBackEnabled: Boolean,
    isNavigationBarPadded: Boolean,
    onDismissRequest: () -> Unit,
    loadMoreState: InfiniteScrollState,
    loadMoreEnabled: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    floatingAction: (@Composable () -> Unit)? = null,
    collapsedHeightFraction: Float = COLLAPSED_HEIGHT_FRACTION,
    content: LazyListScope.() -> Unit,
) {
    key(shopId) {
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
        val handleDragState = rememberShopDetailHandleDragState(bottomSheetState, onDismissRequest)

        NavigationBackHandler(
            state = backEventState,
            isBackEnabled = isBackEnabled,
            onBackCompleted = onDismissRequest,
        )

        val isFullyExpanded =
            bottomSheetState.currentValue == SheetValue.Expanded &&
                bottomSheetState.targetValue == SheetValue.Expanded &&
                !bottomSheetState.isAnimationRunning

        Box(modifier = modifier.fillMaxSize()) {
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
                        ).graphicsLayer { translationY = handleDragState.offsetPx },
                scaffoldState = scaffoldState,
                sheetPeekHeight = maxHeight * collapsedHeightFraction,
                sheetMaxWidth = Dp.Unspecified,
                sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                sheetContainerColor = CommonColor.White,
                sheetShadowElevation = 16.dp,
                containerColor = Color.Transparent,
                sheetDragHandle = null,
                sheetContent = {
                    InfiniteLazyColumn(
                        loadMoreState = loadMoreState,
                        loadMoreEnabled = loadMoreEnabled,
                        onLoadMore = onLoadMore,
                        state = listState,
                        modifier =
                            Modifier
                                .testTag("shop_detail_list")
                                .fillMaxWidth()
                                .height(maxHeight)
                                .run {
                                    if (isNavigationBarPadded) navigationBarsPadding() else this
                                },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item(key = "shop_detail_handle", contentType = "handle") {
                            SheetHandle(dragModifier = handleDragState.modifier)
                        }
                        content()
                        if (floatingAction != null && isFullyExpanded) {
                            item(key = "shop_detail_floating_action_spacer", contentType = "spacer") {
                                Spacer(modifier = Modifier.height(FLOATING_ACTION_BOTTOM_SPACE))
                            }
                        }
                    }
                },
                content = {},
            )

            if (floatingAction != null && isFullyExpanded) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .run {
                                if (isNavigationBarPadded) navigationBarsPadding() else this
                            }.padding(bottom = 12.dp)
                            .graphicsLayer { translationY = handleDragState.offsetPx },
                ) {
                    floatingAction()
                }
            }
        }
    }
}

@Composable
private fun SheetHandle(dragModifier: Modifier) {
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
private val FLOATING_ACTION_BOTTOM_SPACE = 70.dp
