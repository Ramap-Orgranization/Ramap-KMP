package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel

@Immutable
data class ShopOverviewHeaderUiState(
    val waitingSystem: WaitingSystemUiModel? = null,
    val isBookmarked: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val showNotificationActions: Boolean = true,
    val isHidden: Boolean = false,
    val isAppleMapsAvailable: Boolean = false,
)
