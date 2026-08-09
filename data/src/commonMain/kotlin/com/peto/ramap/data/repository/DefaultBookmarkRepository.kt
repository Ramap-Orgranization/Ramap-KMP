package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultBookmarkRepository(
    private val dataSource: BookmarkShopDataSource,
) : BookmarkRepository {
    override suspend fun fetchBookmarkedShopIds(): RamapResult<Set<String>> =
        invokeRequest { dataSource.fetchBookmarkedShopIds().mapTo(mutableSetOf()) { it.shopId } }

    override suspend fun addBookmark(shopId: String): RamapResult<Unit> = invokeRequest { dataSource.addBookmark(shopId) }

    override suspend fun addBookmarks(shopIds: Set<String>): RamapResult<Unit> = invokeRequest { dataSource.addBookmarks(shopIds) }

    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> = invokeRequest { dataSource.removeBookmark(shopId) }
}
