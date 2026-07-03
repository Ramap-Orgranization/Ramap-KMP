package com.peto.ramap.data.datasource.personalization

import com.peto.ramap.data.model.PersonalizationRequest
import com.peto.ramap.data.model.PersonalizationResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class RemoteHiddenShopDataSource(
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
    }
}
