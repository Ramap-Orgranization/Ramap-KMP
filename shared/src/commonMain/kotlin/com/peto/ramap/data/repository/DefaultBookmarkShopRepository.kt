package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.domain.repository.BookmarkShopRepository

class DefaultBookmarkShopRepository(
    private val dataSource: BookmarkShopDataSource,
) : BookmarkShopRepository {
    override suspend fun fetchBookmarkedShopIds(): Set<String> = dataSource.fetchBookmarkedShopIds().map { it.shopId }.toSet()

    override suspend fun addBookmark(shopId: String) {
        dataSource.addBookmark(shopId)
    }

    override suspend fun removeBookmark(shopId: String) {
        dataSource.removeBookmark(shopId)
    }
}
