package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult

interface BookmarkRepository {
    suspend fun fetchBookmarkedShopIds(accountId: String?): RamapResult<Set<String>>

    suspend fun addBookmarks(
        shopIds: Set<String>,
        accountId: String?,
    ): RamapResult<Unit>

    suspend fun removeBookmark(
        shopId: String,
        accountId: String?,
    ): RamapResult<Unit>

    suspend fun synchronizeGuestBookmarks(accountId: String): RamapResult<Unit>
}
