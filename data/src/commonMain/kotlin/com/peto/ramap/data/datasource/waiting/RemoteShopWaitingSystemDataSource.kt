package com.peto.ramap.data.datasource.waiting

import com.peto.ramap.data.model.ShopWaitingSystemResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

internal class RemoteShopWaitingSystemDataSource(
    private val client: SupabaseClient,
) : ShopWaitingSystemDataSource {
    override suspend fun fetchShopWaitingSystem(shopId: String): ShopWaitingSystemResponse? {
        val result =
            client
                .from(TABLE_NAME)
                .select(columns = Columns.list(COLUMN_WAITING_PROVIDER, COLUMN_WAITING_PROVIDER_URL)) {
                    filter {
                        eq(COLUMN_ID, shopId)
                    }
                    limit(1)
                }

        return result.decodeSingleOrNull<ShopWaitingSystemResponse>()
    }

    companion object {
        private const val TABLE_NAME = "ramen_shops"
        private const val COLUMN_ID = "id"
        private const val COLUMN_WAITING_PROVIDER = "waiting_provider"
        private const val COLUMN_WAITING_PROVIDER_URL = "waiting_provider_url"
    }
}
