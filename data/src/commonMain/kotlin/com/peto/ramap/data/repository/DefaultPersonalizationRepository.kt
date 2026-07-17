package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DefaultPersonalizationRepository(
    private val bookmarkDataSource: BookmarkShopDataSource,
    private val hiddenShopDataSource: HiddenShopDataSource,
) : PersonalizationRepository {
    override suspend fun fetchPersonalization(): RamapResult<Personalization> =
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

    override suspend fun addBookmark(shopId: String): RamapResult<Unit> = invokeRequest { bookmarkDataSource.addBookmark(shopId) }

    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> = invokeRequest { bookmarkDataSource.removeBookmark(shopId) }

    override suspend fun hideShop(
        shopId: String,
        removeBookmark: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            if (removeBookmark) {
                hiddenShopDataSource.hideBookmarkedShop(shopId)
            } else {
                hiddenShopDataSource.hideShop(shopId)
            }
        }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> = invokeRequest { hiddenShopDataSource.unhideShop(shopId) }
}
