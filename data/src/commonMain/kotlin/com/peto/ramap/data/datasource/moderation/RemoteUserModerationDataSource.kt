package com.peto.ramap.data.datasource.moderation

import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.BlockedUserResponse
import com.peto.ramap.data.model.SignedImageRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteUserModerationDataSource(
    private val client: SupabaseClient,
    private val imageUrlDataSource: ImageUrlDataSource,
) : UserModerationDataSource {
    override suspend fun fetchBlockedUsers(): List<BlockedUserResponse> {
        val profiles =
            client.postgrest
                .rpc(RPC_FETCH_BLOCKED_USERS)
                .decodeList<BlockedUserResponse>()
        val images = profiles.mapNotNull { it.avatarPath }.map { SignedImageRequest(BUCKET_PROFILE_AVATARS, it) }
        val urls = imageUrlDataSource.fetchUrls(images)
        return profiles.map { profile ->
            profile.copy(avatarUrl = profile.avatarPath?.let { urls[SignedImageRequest(BUCKET_PROFILE_AVATARS, it)]?.url })
        }
    }

    override suspend fun report(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
    ) {
        client.postgrest.rpc(
            RPC_REPORT_CONTENT,
            buildJsonObject {
                put(PARAM_TARGET_TYPE, targetType)
                put(PARAM_TARGET_ID, targetId)
                put(PARAM_REASON, reason)
                put(PARAM_DETAILS, details)
            },
        )
    }

    override suspend fun changeBlock(
        userId: String,
        blocked: Boolean,
    ) {
        client.postgrest.rpc(
            RPC_CHANGE_USER_BLOCK,
            buildJsonObject {
                put(PARAM_USER_ID, userId)
                put(PARAM_BLOCKED, blocked)
            },
        )
    }

    private companion object {
        const val RPC_FETCH_BLOCKED_USERS = "fetch_blocked_community_profiles_v2"
        const val RPC_REPORT_CONTENT = "report_community_content"
        const val RPC_CHANGE_USER_BLOCK = "change_community_block"
        const val PARAM_USER_ID = "p_user_id"
        const val PARAM_TARGET_TYPE = "p_target_type"
        const val PARAM_TARGET_ID = "p_target_id"
        const val PARAM_REASON = "p_reason"
        const val PARAM_DETAILS = "p_details"
        const val PARAM_BLOCKED = "p_blocked"
        const val BUCKET_PROFILE_AVATARS = "profile-avatars"
    }
}
