package com.peto.ramap.data.datasource.follow

import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.FollowCountsResponse
import com.peto.ramap.data.model.PublicProfileResponse
import com.peto.ramap.data.model.SignedImageRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteFollowDataSource(
    private val client: SupabaseClient,
    private val images: ImageUrlDataSource,
) : FollowDataSource {
    override suspend fun fetchCounts(): FollowCountsResponse =
        client.postgrest
            .rpc(FUNCTION_FETCH_MY_FOLLOW_COUNTS)
            .decodeAs<FollowCountsResponse>()

    override suspend fun fetchConnections(
        list: String,
        offset: Long,
    ): List<PublicProfileResponse> {
        val profiles =
            client.postgrest
                .rpc(
                    FUNCTION_FETCH_MY_FOLLOW_CONNECTIONS,
                    buildJsonObject {
                        put(PARAMETER_P_LIST, list)
                        put(PARAMETER_P_OFFSET, offset)
                    },
                ).decodeList<PublicProfileResponse>()
        return resolveAvatarUrls(profiles)
    }

    override suspend fun changeFollow(
        userId: String,
        action: String,
    ) {
        client.postgrest.rpc(
            FUNCTION_CHANGE_PROFILE_FOLLOW,
            buildJsonObject {
                put(PARAMETER_P_USER_ID, userId)
                put(PARAMETER_P_ACTION, action)
            },
        )
    }

    private suspend fun resolveAvatarUrls(profiles: List<PublicProfileResponse>): List<PublicProfileResponse> {
        val requests =
            profiles.mapNotNull { profile ->
                profile.avatarPath?.let { SignedImageRequest(BUCKET_PROFILE_AVATARS, it) }
            }
        val urls = images.fetchUrls(requests)
        return profiles.map { profile ->
            profile.copy(
                avatarUrl = profile.avatarPath?.let { urls[SignedImageRequest(BUCKET_PROFILE_AVATARS, it)]?.url },
            )
        }
    }

    private companion object {
        const val BUCKET_PROFILE_AVATARS = "profile-avatars"

        const val FUNCTION_FETCH_MY_FOLLOW_COUNTS = "fetch_my_follow_counts"
        const val FUNCTION_FETCH_MY_FOLLOW_CONNECTIONS = "fetch_my_follow_connections"
        const val FUNCTION_CHANGE_PROFILE_FOLLOW = "change_profile_follow"

        const val PARAMETER_P_LIST = "p_list"
        const val PARAMETER_P_OFFSET = "p_offset"
        const val PARAMETER_P_USER_ID = "p_user_id"
        const val PARAMETER_P_ACTION = "p_action"
    }
}
