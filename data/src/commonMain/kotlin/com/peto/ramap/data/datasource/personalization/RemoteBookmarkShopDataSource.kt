package com.peto.ramap.data.datasource.personalization

import com.peto.ramap.data.model.AccountBookmarkRequest
import com.peto.ramap.data.model.PersonalizationResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

internal class RemoteBookmarkShopDataSource(
    private val client: SupabaseClient,
) : BookmarkShopDataSource {
    override suspend fun fetchBookmarkedShopIds(accountId: String): Set<String> {
        val shopIds = mutableSetOf<String>()
        var offset = 0L
        do {
            val page =
                client
                    .from(TABLE_NAME)
                    .select(columns = Columns.list(COLUMN_SHOP_ID)) {
                        filter { eq(COLUMN_USER_ID, accountId) }
                        order(COLUMN_SHOP_ID, Order.ASCENDING)
                        range(offset, offset + PAGE_SIZE - 1)
                    }.decodeList<PersonalizationResponse>()
            page.mapTo(shopIds) { it.shopId }
            offset += page.size
        } while (page.size == PAGE_SIZE)
        return shopIds
    }

    override suspend fun addBookmarks(
        shopIds: Set<String>,
        accountId: String,
    ) {
        if (shopIds.isEmpty()) return
        client.from(TABLE_NAME).upsert(shopIds.map { AccountBookmarkRequest(accountId, it) }) {
            onConflict = "$COLUMN_USER_ID,$COLUMN_SHOP_ID"
            ignoreDuplicates = true
        }
    }

    override suspend fun removeBookmark(
        shopId: String,
        accountId: String,
    ) {
        client.from(TABLE_NAME).delete {
            filter {
                eq(COLUMN_USER_ID, accountId)
                eq(COLUMN_SHOP_ID, shopId)
            }
        }
    }

    private companion object {
        const val TABLE_NAME = "user_shop_bookmarks"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_SHOP_ID = "shop_id"
        const val PAGE_SIZE = 100
    }
}
