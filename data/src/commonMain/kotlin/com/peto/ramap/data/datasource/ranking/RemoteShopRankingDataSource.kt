package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.data.model.ShopRankingResponse
import com.peto.ramap.domain.model.rank.RankingPage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

internal class RemoteShopRankingDataSource(
    private val client: SupabaseClient,
) : ShopRankingDataSource {
    override suspend fun fetchShopRankings(parameter: ShopRankingParameters): RankingPage {
        val responses =
            client.postgrest
                .rpc(
                    function = FUNCTION_NAME,
                    parameters = parameter,
                ).decodeList<ShopRankingResponse>()
        return responses.toDomain(parameter.limit)
    }

    companion object {
        private const val FUNCTION_NAME = "fetch_shop_rankings"
    }
}
