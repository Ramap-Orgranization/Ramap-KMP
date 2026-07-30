package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.HiddenShopRepository

class FakeHiddenShopRepository(
    initialShopIds: Set<String> = emptySet(),
    private val shouldFailUpdate: Boolean = false,
    private val onAtomicHide: (suspend () -> Unit)? = null,
) : HiddenShopRepository {
    val shopIds = initialShopIds.toMutableSet()
    val atomicHideRequests = mutableListOf<String>()

    override suspend fun fetchHiddenShopIds() = RamapResult.Success(shopIds.toSet())

    override suspend fun hideShop(shopId: String): RamapResult<Unit> {
        if (shouldFailUpdate) return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
        shopIds += shopId
        return RamapResult.Success(Unit)
    }

    override suspend fun hideBookmarkedShop(shopId: String): RamapResult<Unit> {
        atomicHideRequests += shopId
        onAtomicHide?.invoke()
        return hideShop(shopId)
    }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> {
        if (shouldFailUpdate) return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
        shopIds -= shopId
        return RamapResult.Success(Unit)
    }
}
