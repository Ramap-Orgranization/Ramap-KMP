package com.peto.ramap.data.datasource.menu

import com.peto.ramap.data.model.ShopMenuResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteShopMenuDataSource(
    private val client: SupabaseClient,
) : ShopMenuDataSource {
    override suspend fun fetchShopMenu(shopId: String): ShopMenuResponse =
        client.postgrest
            .rpc(
                function = FETCH_SHOP_MENU_RPC,
                parameters = buildJsonObject { put(SHOP_ID_PARAMETER, shopId) },
            ).decodeList<ShopMenuResponse>()
            .single()

    private companion object {
        const val FETCH_SHOP_MENU_RPC = "fetch_shop_menu"
        const val SHOP_ID_PARAMETER = "p_shop_id"
    }
}
