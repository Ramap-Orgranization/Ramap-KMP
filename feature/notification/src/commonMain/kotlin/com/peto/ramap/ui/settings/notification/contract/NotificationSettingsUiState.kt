package com.peto.ramap.ui.settings.notification.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.settings.notification.model.NotificationRemovalTarget

data class NotificationSettingsUiState(
    val loadState: LoadState<Unit> = LoadState.Idle,
    val areEnabled: Boolean = false,
    val subscribedShopCount: Int = 0,
    val shops: RamenShops = RamenShops(emptyMap()),
    val subscribedEvents: List<ShopEvent> = emptyList(),
    val pendingRemoval: NotificationRemovalTarget? = null,
) : State
