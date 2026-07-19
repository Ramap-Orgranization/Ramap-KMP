package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.SubscribedShopRepository

class FakeSubscribedShopRepository(
    initialShopIds: Set<String> = emptySet(),
    private val shouldFailUpdate: Boolean = false,
) : SubscribedShopRepository {
    val shopIds = initialShopIds.toMutableSet()
    val subscriptionRequests = mutableListOf<String>()
    val unsubscriptionRequests = mutableListOf<String>()

    override suspend fun fetchSubscribedShopIds() = RamapResult.Success(shopIds.toSet())

    override suspend fun subscribeShop(shopId: String): RamapResult<Unit> {
        subscriptionRequests += shopId
        if (shouldFailUpdate) return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
        shopIds += shopId
        return RamapResult.Success(Unit)
    }

    override suspend fun unsubscribeShop(shopId: String): RamapResult<Unit> {
        unsubscriptionRequests += shopId
        if (shouldFailUpdate) return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
        shopIds -= shopId
        return RamapResult.Success(Unit)
    }
}
