package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult

interface BookmarkRepository {
    suspend fun fetchBookmarkedShopIds(): RamapResult<Set<String>>

    suspend fun addBookmark(shopId: String): RamapResult<Unit>

    suspend fun removeBookmark(shopId: String): RamapResult<Unit>
}
