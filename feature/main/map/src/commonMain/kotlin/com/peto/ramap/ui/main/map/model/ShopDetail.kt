package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.ShopWaitingSystem

data class ShopDetail(
    val shop: RamenShop,
    val waitingSystem: ShopWaitingSystem?,
    val event: ShopEvent?,
)
