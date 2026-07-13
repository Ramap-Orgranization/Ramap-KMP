package com.peto.ramap.ui.hidden.contract

import com.peto.ramap.core.base.State
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.ui.common.LoadState

data class HiddenShopListUiState(
    val shopsState: LoadState<List<RamenShop>> = LoadState.Idle,
) : State
