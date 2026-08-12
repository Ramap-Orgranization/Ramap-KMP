package com.peto.ramap.designsystem.bottomsheet

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.DimmedColor

@Immutable
data class CommonBottomSheetConfig(
    val scrimColor: Color = DimmedColor.D020,
    val shape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    val showHandle: Boolean = true,
    val isDraggable: Boolean = false,
    val handleTopPadding: Dp = 11.dp,
    val handleBottomPadding: Dp = 11.dp,
    val maxHeightFraction: Float = 0.8f,
    val maxHeight: Dp? = null,
    val dismissOnScrimClick: Boolean = true,
)
