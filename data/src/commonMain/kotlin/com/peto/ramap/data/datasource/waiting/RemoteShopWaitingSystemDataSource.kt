package com.peto.ramap.data.datasource.waiting

import com.peto.ramap.data.model.ShopWaitingSystemResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class RemoteShopWaitingSystemDataSource(
    private val client: SupabaseClient,
) : ShopWaitingSystemDataSource {
    override suspend fun fetchShopWaitingSystem(shopId: String): ShopWaitingSystemResponse? {
        val result =
            client
                .from(TABLE_NAME)
                .select {
                    filter {
                        eq(COLUMN_SHOP_ID, shopId)
                    }
                    limit(1)
                }

        return result.decodeSingleOrNull<ShopWaitingSystemResponse>()
    }

    companion object {
        private const val TABLE_NAME = "shop_waiting_systems"
        private const val COLUMN_SHOP_ID = "shop_id"
    }
}
