package com.peto.ramap.data.datasource.waiting

import com.peto.ramap.data.model.ShopWaitingSystemResponse

interface ShopWaitingSystemDataSource {
    suspend fun fetchShopWaitingSystem(shopId: String): ShopWaitingSystemResponse?
}
