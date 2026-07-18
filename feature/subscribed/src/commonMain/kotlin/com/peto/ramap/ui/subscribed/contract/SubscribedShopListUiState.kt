package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.common.LoadState

data class SubscribedShopListUiState(
    val shopsState: LoadState<RamenShops> = LoadState.Idle,
    val subscribedEvents: List<ShopEvent> = emptyList(),
) : State
