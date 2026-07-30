package com.peto.ramap.data.datasource.personalization

import com.peto.ramap.data.model.PersonalizationRequest
import com.peto.ramap.data.model.PersonalizationResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

internal class RemoteBookmarkShopDataSource(
    private val client: SupabaseClient,
) : BookmarkShopDataSource {
    override suspend fun fetchBookmarkedShopIds(): List<PersonalizationResponse> =
        client
            .from(TABLE_NAME)
            .select(columns = Columns.list(COLUMN_SHOP_ID))
            .decodeList()

    override suspend fun addBookmark(shopId: String) {
        client
            .from(TABLE_NAME)
            .upsert(value = PersonalizationRequest(shopId)) {
                onConflict = "$COLUMN_USER_ID,$COLUMN_SHOP_ID"
                ignoreDuplicates = true
            }
    }

    override suspend fun removeBookmark(shopId: String) {
        client
            .from(TABLE_NAME)
            .delete {
                filter {
                    eq(COLUMN_SHOP_ID, shopId)
                }
            }
    }

    companion object {
        private const val TABLE_NAME = "user_shop_bookmarks"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_SHOP_ID = "shop_id"
    }
}
