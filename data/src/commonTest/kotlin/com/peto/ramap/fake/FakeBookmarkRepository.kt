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

    override suspend fun fetchBookmarkedShopIds(accountId: String?) = RamapResult.Success(shopIds.toSet())

    override suspend fun addBookmarks(
        shopIds: Set<String>,
        accountId: String?,
    ): RamapResult<Unit> {
        bulkAddRequests += shopIds
        if (shouldFailUpdate) return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
        this.shopIds += shopIds
        return RamapResult.Success(Unit)
    }

    override suspend fun removeBookmark(
        shopId: String,
        accountId: String?,
    ): RamapResult<Unit> {
        removeRequests += shopId
        return update(shopId, false)
    }

    override suspend fun synchronizeGuestBookmarks(accountId: String): RamapResult<Unit> = RamapResult.Success(Unit)

    private fun update(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        if (shouldFailUpdate) return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
        if (enabled) shopIds += shopId else shopIds -= shopId
        return RamapResult.Success(Unit)
    }
}
