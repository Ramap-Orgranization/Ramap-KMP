package com.peto.ramap.data.datasource.personalization

import com.peto.ramap.data.model.PersonalizationResponse

internal interface BookmarkShopDataSource {
    suspend fun fetchBookmarkedShopIds(): List<PersonalizationResponse>

    suspend fun addBookmark(shopId: String)

    suspend fun addBookmarks(shopIds: Set<String>)

    suspend fun removeBookmark(shopId: String)
}
