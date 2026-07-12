package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.ShopWaitingSystem
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository

class FakeShopWaitingSystemRepository(
    private val result: ShopWaitingSystem? = null,
    private val error: RamapError? = null,
) : ShopWaitingSystemRepository {
    val requestedShopIds = mutableListOf<String>()

    override suspend fun fetchShopWaitingSystem(shopId: String): RamapResult<ShopWaitingSystem?> {
        requestedShopIds += shopId
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(result)
    }
}
