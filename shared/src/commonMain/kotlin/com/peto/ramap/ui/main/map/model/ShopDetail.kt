package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.domain.model.ShopWaitingSystem

data class ShopDetail(
    val shop: RamenShop,
    val waitingSystem: ShopWaitingSystem?,
    val event: ShopEvent?,
)
