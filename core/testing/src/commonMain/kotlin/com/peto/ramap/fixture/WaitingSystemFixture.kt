package com.peto.ramap.fixture

import com.peto.ramap.domain.model.shop.WaitingProvider
import com.peto.ramap.domain.model.shop.WaitingSystem

fun waitingSystemFixture(shopId: String): WaitingSystem =
    WaitingSystem(
        id = "waiting-$shopId",
        shopId = shopId,
        provider = WaitingProvider.CATCHTABLE,
        providerUrl = "https://app.catchtable.co.kr/ct/shop/$shopId",
    )
