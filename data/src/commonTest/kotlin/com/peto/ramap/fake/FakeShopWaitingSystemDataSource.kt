package com.peto.ramap.fake

import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import com.peto.ramap.data.model.ShopWaitingSystemResponse

class FakeShopWaitingSystemDataSource(
    private val response: ShopWaitingSystemResponse? = null,
    private val error: Throwable? = null,
) : ShopWaitingSystemDataSource {
    var requestedShopId: String? = null
        private set

    override suspend fun fetchShopWaitingSystem(shopId: String): ShopWaitingSystemResponse? {
        requestedShopId = shopId
        error?.let { throw it }
        return response
    }
}
