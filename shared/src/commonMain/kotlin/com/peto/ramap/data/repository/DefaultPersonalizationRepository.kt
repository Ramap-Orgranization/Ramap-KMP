package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.domain.model.Personalization
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.network.execute.invokeRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DefaultPersonalizationRepository(
    private val bookmarkDataSource: BookmarkShopDataSource,
    private val hiddenShopDataSource: HiddenShopDataSource,
    private val supabaseClient: SupabaseClient,
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
                supabaseClient.postgrest.rpc(
                    HIDE_SHOP_RPC,
                    buildJsonObject { put("p_shop_id", shopId) },
                )
            } else {
                hiddenShopDataSource.hideShop(shopId)
            }
        }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> = invokeRequest { hiddenShopDataSource.unhideShop(shopId) }

    private companion object {
        const val HIDE_SHOP_RPC = "hide_shop_and_remove_bookmark"
    }
}
