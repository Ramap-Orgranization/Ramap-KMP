package com.peto.ramap.ui.hidden.contract

import com.peto.ramap.ui.base.State
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.common.LoadState

data class HiddenShopListUiState(
    val shopsState: LoadState<RamenShops> = LoadState.Idle,
) : State
