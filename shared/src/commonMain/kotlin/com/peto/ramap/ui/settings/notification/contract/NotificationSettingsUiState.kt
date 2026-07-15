package com.peto.ramap.ui.settings.notification.contract

import com.peto.ramap.core.base.State
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.ui.settings.notification.model.NotificationRemovalTarget

data class NotificationSettingsUiState(
    val areEnabled: Boolean = false,
    val subscribedShopCount: Int = 0,
    val shops: List<RamenShop> = emptyList(),
    val subscribedEvents: List<ShopEvent> = emptyList(),
    val pendingRemoval: NotificationRemovalTarget? = null,
) : State
