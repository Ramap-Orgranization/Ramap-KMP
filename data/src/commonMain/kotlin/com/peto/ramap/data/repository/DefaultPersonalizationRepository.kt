package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DefaultPersonalizationRepository(
    private val bookmarkDataSource: BookmarkShopDataSource,
    private val hiddenShopDataSource: HiddenShopDataSource,
) : PersonalizationRepository {
    private val mutableBookmarkedShopIds = MutableStateFlow<Set<String>>(emptySet())
    override val bookmarkedShopIds = mutableBookmarkedShopIds.asStateFlow()

    override suspend fun fetchPersonalization(): RamapResult<Personalization> {
        val result =
            invokeRequest {
                coroutineScope {
                    val bookmarks =
                        async {
                            bookmarkDataSource.fetchBookmarkedShopIds().map { it.shopId }.toSet()
                        }
                    val hiddenShops =
                        async {
                            hiddenShopDataSource.fetchHiddenShopIds().map { it.shopId }.toSet()
                        }

                    Personalization(
                        bookmarkedShopIds = bookmarks.await(),
                        hiddenShopIds = hiddenShops.await(),
                    )
                }
            }
        if (result is RamapResult.Success) {
            mutableBookmarkedShopIds.value = result.data.bookmarkedShopIds
        }
        return result
    }

    override suspend fun addBookmark(shopId: String): RamapResult<Unit> {
        val result = invokeRequest { bookmarkDataSource.addBookmark(shopId) }
        if (result is RamapResult.Success) {
            mutableBookmarkedShopIds.update { it + shopId }
        }
        return result
    }

    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> {
        val result = invokeRequest { bookmarkDataSource.removeBookmark(shopId) }
        if (result is RamapResult.Success) {
            mutableBookmarkedShopIds.update { it - shopId }
        }
        return result
    }

    override suspend fun hideShop(
        shopId: String,
        removeBookmark: Boolean,
    ): RamapResult<Unit> {
        val result =
            invokeRequest {
                if (removeBookmark) {
                    hiddenShopDataSource.hideBookmarkedShop(shopId)
                } else {
                    hiddenShopDataSource.hideShop(shopId)
                }
            }
        if (result is RamapResult.Success && removeBookmark) {
            mutableBookmarkedShopIds.update { it - shopId }
        }
        return result
    }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> = invokeRequest { hiddenShopDataSource.unhideShop(shopId) }
}
