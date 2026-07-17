package com.peto.ramap.ui.subscribed.contract

import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget

data class SubscribedShopListUiState(
    val shopsState: LoadState<RamenShops> = LoadState.Idle,
    val pendingRemoval: SubscribedRemovalTarget? = null,
) : State
