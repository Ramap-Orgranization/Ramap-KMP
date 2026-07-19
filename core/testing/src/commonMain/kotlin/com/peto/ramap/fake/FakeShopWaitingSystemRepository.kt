package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository

class FakeShopWaitingSystemRepository(
    private val result: WaitingSystem? = null,
    private val error: RamapError? = null,
) : ShopWaitingSystemRepository {
    val requestedShopIds = mutableListOf<String>()

    override suspend fun fetchShopWaitingSystem(shopId: String): RamapResult<WaitingSystem?> {
        requestedShopIds += shopId
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(result)
    }
}
