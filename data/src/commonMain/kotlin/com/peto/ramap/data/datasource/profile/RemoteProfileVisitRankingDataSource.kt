package com.peto.ramap.data.datasource.profile

import com.peto.ramap.data.model.ProfileVisitRankingResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteProfileVisitRankingDataSource(
    private val client: SupabaseClient,
) : ProfileVisitRankingDataSource {
    override suspend fun fetchProfileVisitRanking(userId: String): ProfileVisitRankingResponse =
        client.postgrest
            .rpc(
                function = RPC_FETCH_PROFILE_VISIT_RANKING,
                parameters = buildJsonObject { put(PARAM_USER_ID, userId) },
            ).decodeAs<ProfileVisitRankingResponse>()

    private companion object {
        const val RPC_FETCH_PROFILE_VISIT_RANKING = "fetch_profile_visit_ranking"
        const val PARAM_USER_ID = "p_user_id"
    }
}
