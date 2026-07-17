package com.peto.ramap.ui.bookmark.contract

import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.common.LoadState

data class BookmarkedShopListUiState(
    val shopsState: LoadState<RamenShops> = LoadState.Idle,
    val pendingBookmarkShopId: String? = null,
) : State
