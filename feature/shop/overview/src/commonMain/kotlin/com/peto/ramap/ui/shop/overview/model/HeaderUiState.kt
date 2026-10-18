package com.peto.ramap.ui.shop.overview.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel

@Immutable
data class HeaderUiState(
    val waitingSystem: WaitingSystemUiModel? = null,
    val isBookmarked: Boolean,
    val isNotificationEnabled: Boolean = false,
    val showNotificationActions: Boolean = true,
    val isHidden: Boolean = false,
    val isAppleMapsAvailable: Boolean = false,
)
