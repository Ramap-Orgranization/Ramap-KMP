package com.peto.ramap.data.datasource.onboarding

import com.peto.ramap.data.model.ShopSuggestionsPageResponse
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsCursor
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsPage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteShopSuggestionsDataSource(
    private val client: SupabaseClient,
) : ShopSuggestionsDataSource {
    override suspend fun fetchSuggestions(
        cursor: ShopSuggestionsCursor?,
        limit: Int,
    ): ShopSuggestionsPage =
        client.postgrest
            .rpc(
                function = FUNCTION_NAME,
                parameters =
                    buildJsonObject {
                        put("p_limit", limit)
                        put("p_cursor", cursor?.value)
                    },
            ).decodeAs<ShopSuggestionsPageResponse>()
            .toDomain()

    companion object {
        private const val FUNCTION_NAME = "fetch_onboarding_shop_suggestions"
    }
}
