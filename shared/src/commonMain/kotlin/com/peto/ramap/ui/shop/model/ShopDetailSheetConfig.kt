package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
data class ShopDetailSheetConfig(
    val isBackEnabled: Boolean,
    val maxHeight: Dp,
    val isNavigationBarPadded: Boolean = false,
    val visible: Boolean = true,
    val showRequestedLoadingInSheet: Boolean = false,
    val showReviewsOnOpen: Boolean = false,
)
