package com.peto.ramap.domain.usecase

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.WaitingSystem

data class ShopDetail(
    val shop: RamenShop,
    val waitingSystem: WaitingSystem?,
    val event: ShopEvent?,
)
