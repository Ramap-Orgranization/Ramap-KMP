package com.peto.ramap.data.datasource.community

import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.model.MyCommunityProfileResponse
import com.peto.ramap.data.model.PublicProfileResponse
import com.peto.ramap.data.model.ReviewResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes

internal class RemoteCommunityDataSource(
    private val client: SupabaseClient,
    private val reviewDataSource: ReviewDataSource,
) : CommunityDataSource {
    override suspend fun fetchMyCommunityProfile(): MyCommunityProfileResponse {
        val profile =
            client.postgrest
                .rpc(RPC_FETCH_MY_COMMUNITY_MEMBERSHIP)
                .decodeAs<MyCommunityProfileResponse>()
        return profile.copy(avatarUrl = signedAvatar(profile.avatarPath))
    }

    override suspend fun fetchPublicProfile(userId: String): PublicProfileResponse? {
        val profile =
            client.postgrest
                .rpc(
                    RPC_FETCH_PUBLIC_COMMUNITY_PROFILE,
                    buildJsonObject { put(PARAM_USER_ID, userId) },
                ).decodeAs<PublicProfileResponse?>() ?: return null
        return profile.copy(avatarUrl = signedAvatar(profile.avatarPath))
    }

    private suspend fun signedAvatar(path: String?): String? {
        if (path == null) return null
        return try {
            client.storage.from(BUCKET_PROFILE_AVATARS).createSignedUrl(path, SIGNED_URL_LIFETIME)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): List<ReviewResponse> = reviewDataSource.fetchProfileReviews(userId, offset)

    override suspend fun fetchBlockedUsers(): List<PublicProfileResponse> = client.postgrest.rpc(RPC_FETCH_BLOCKED_COMMUNITY_PROFILES).decodeList()

    override suspend fun report(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
    ) {
        client.postgrest.rpc(
            RPC_REPORT_COMMUNITY_CONTENT,
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
            RPC_CHANGE_COMMUNITY_BLOCK,
            buildJsonObject {
                put(PARAM_USER_ID, userId)
                put(PARAM_BLOCKED, blocked)
            },
        )
    }

    private companion object {
        const val RPC_FETCH_MY_COMMUNITY_MEMBERSHIP = "fetch_my_community_membership"
        const val RPC_FETCH_PUBLIC_COMMUNITY_PROFILE = "fetch_public_community_profile"
        const val RPC_FETCH_BLOCKED_COMMUNITY_PROFILES = "fetch_blocked_community_profiles"
        const val RPC_REPORT_COMMUNITY_CONTENT = "report_community_content"
        const val RPC_CHANGE_COMMUNITY_BLOCK = "change_community_block"

        const val PARAM_USER_ID = "p_user_id"
        const val PARAM_TARGET_TYPE = "p_target_type"
        const val PARAM_TARGET_ID = "p_target_id"
        const val PARAM_REASON = "p_reason"
        const val PARAM_DETAILS = "p_details"
        const val PARAM_BLOCKED = "p_blocked"

        const val BUCKET_PROFILE_AVATARS = "profile-avatars"
        val SIGNED_URL_LIFETIME = 5.minutes
    }
}
