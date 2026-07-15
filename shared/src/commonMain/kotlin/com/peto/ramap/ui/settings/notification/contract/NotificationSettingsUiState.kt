package com.peto.ramap.ui.settings.notification.contract

import com.peto.ramap.core.base.State
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.ShopEvent

data class NotificationSettingsUiState(
    val areEnabled: Boolean = true,
    val subscribedShopCount: Int = 0,
    val shops: List<RamenShop> = emptyList(),
    val subscribedEvents: List<ShopEvent> = emptyList(),
    val pendingRemoval: NotificationRemovalTarget? = null,
) : State
