package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.BookmarkRepository

class FakeBookmarkRepository(
    initialShopIds: Set<String> = emptySet(),
    private val shouldFailUpdate: Boolean = false,
) : BookmarkRepository {
    val shopIds = initialShopIds.toMutableSet()
    val removeRequests = mutableListOf<String>()
    val bulkAddRequests = mutableListOf<Set<String>>()

    override suspend fun fetchBookmarkedShopIds() = RamapResult.Success(shopIds.toSet())

    override suspend fun addBookmark(shopId: String): RamapResult<Unit> = update(shopId, true)

    override suspend fun addBookmarks(shopIds: Set<String>): RamapResult<Unit> {
        bulkAddRequests += shopIds
        if (shouldFailUpdate) return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
        this.shopIds += shopIds
        return RamapResult.Success(Unit)
    }

    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> {
        removeRequests += shopId
        return update(shopId, false)
    }

    private fun update(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        if (shouldFailUpdate) return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
        if (enabled) shopIds += shopId else shopIds -= shopId
        return RamapResult.Success(Unit)
    }
}
