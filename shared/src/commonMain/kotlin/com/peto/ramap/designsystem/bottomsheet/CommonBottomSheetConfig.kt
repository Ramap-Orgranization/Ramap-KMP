package com.peto.ramap.designsystem.bottomsheet

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class CommonBottomSheetConfig(
    val scrimColor: Color = Color.Transparent,
    val shape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    val showHandle: Boolean = true,
    val handleTopPadding: Dp = 11.dp,
    val handleBottomPadding: Dp = 11.dp,
    val maxHeightFraction: Float = 0.8f,
    val dismissOnScrimClick: Boolean = true,
)
