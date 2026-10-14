package com.peto.ramap.ui.shop.overview.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.usecase.ShopDetail

@Immutable
data class OverviewUiState(
    val detail: ShopDetail,
    val headerState: HeaderUiState,
)
