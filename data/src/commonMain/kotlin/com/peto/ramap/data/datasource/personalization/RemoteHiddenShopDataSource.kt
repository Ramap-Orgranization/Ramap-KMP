package com.peto.ramap.data.datasource.personalization

import com.peto.ramap.data.model.PersonalizationRequest
import com.peto.ramap.data.model.PersonalizationResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteHiddenShopDataSource(
    private val client: SupabaseClient,
) : HiddenShopDataSource {
    override suspend fun fetchHiddenShopIds(): List<PersonalizationResponse> =
        client
            .from(TABLE_NAME)
            .select(columns = Columns.list(COLUMN_SHOP_ID))
            .decodeList()

    override suspend fun hideShop(shopId: String) {
        client
            .from(TABLE_NAME)
            .upsert(value = PersonalizationRequest(shopId)) {
                onConflict = "$COLUMN_USER_ID,$COLUMN_SHOP_ID"
                ignoreDuplicates = true
            }
    }

    override suspend fun hideBookmarkedShop(shopId: String) {
        client.postgrest.rpc(
            function = HIDE_SHOP_AND_REMOVE_BOOKMARK_RPC,
            parameters = buildJsonObject { put(SHOP_ID_PARAMETER, shopId) },
        )
    }

    override suspend fun unhideShop(shopId: String) {
        client
            .from(TABLE_NAME)
            .delete {
                filter {
                    eq(COLUMN_SHOP_ID, shopId)
                }
            }
    }

    companion object {
        private const val TABLE_NAME = "user_hidden_shops"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_SHOP_ID = "shop_id"
        private const val HIDE_SHOP_AND_REMOVE_BOOKMARK_RPC = "hide_shop_and_remove_bookmark"
        private const val SHOP_ID_PARAMETER = "p_shop_id"
    }
}
