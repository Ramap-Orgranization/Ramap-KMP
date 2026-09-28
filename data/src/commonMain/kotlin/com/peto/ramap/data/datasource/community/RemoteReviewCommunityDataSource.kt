package com.peto.ramap.data.datasource.community

import com.peto.ramap.data.model.PublicProfileResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

internal class RemoteReviewCommunityDataSource(
    private val client: SupabaseClient,
) : ReviewCommunityDataSource {
    override suspend fun fetchBlockedUsers(): List<PublicProfileResponse> =
        client.postgrest
            .rpc(FUNCTION_FETCH_BLOCKED_USERS)
            .decodeList()

    private companion object {
        const val FUNCTION_FETCH_BLOCKED_USERS = "fetch_blocked_community_profiles"
    }
}
