package com.peto.ramap.fixture

import com.peto.ramap.domain.model.shop.ShopWaitingSystem
import com.peto.ramap.domain.model.shop.WaitingProvider

fun waitingSystemFixture(shopId: String): ShopWaitingSystem =
    ShopWaitingSystem(
        id = "waiting-$shopId",
        shopId = shopId,
        provider = WaitingProvider.CATCHTABLE,
        providerUrl = "https://app.catchtable.co.kr/ct/shop/$shopId",
    )
