package com.peto.ramap.domain.repository

interface BookmarkShopRepository {
    suspend fun fetchBookmarkedShopIds(): Set<String>

    suspend fun addBookmark(shopId: String)

    suspend fun removeBookmark(shopId: String)
}
