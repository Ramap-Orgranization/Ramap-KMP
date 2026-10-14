package com.peto.ramap.ui.shop.overview.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.shop.ShopOverView

@Immutable
data class OverviewUiState(
    val overview: ShopOverView,
    val headerState: HeaderUiState,
)
