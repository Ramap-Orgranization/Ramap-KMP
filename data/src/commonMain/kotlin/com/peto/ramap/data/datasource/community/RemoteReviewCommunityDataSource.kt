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

internal class RemoteReviewCommunityDataSource(
    private val client: SupabaseClient,
    private val reviewDataSource: ReviewDataSource,
) : ReviewCommunityDataSource {
    override suspend fun fetchMyCommunityProfile(): MyCommunityProfileResponse {
        val profile =
            client.postgrest
                .rpc("fetch_my_community_membership")
                .decodeAs<MyCommunityProfileResponse>()
        return profile.copy(avatarUrl = signedAvatar(profile.avatarPath))
    }

    override suspend fun fetchPublicProfile(userId: String): PublicProfileResponse? {
        val profile =
            client.postgrest
                .rpc(
                    "fetch_public_community_profile",
                    buildJsonObject { put("p_user_id", userId) },
                ).decodeAs<PublicProfileResponse?>() ?: return null
        return profile.copy(avatarUrl = signedAvatar(profile.avatarPath))
    }

    private suspend fun signedAvatar(path: String?): String? {
        if (path == null) return null
        return try {
            client.storage.from("profile-avatars").createSignedUrl(path, 5.minutes)
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

    override suspend fun fetchBlockedUsers(): List<PublicProfileResponse> = client.postgrest.rpc("fetch_blocked_community_profiles").decodeList()

    override suspend fun report(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
    ) {
        client.postgrest.rpc(
            "report_community_content",
            buildJsonObject {
                put("p_target_type", targetType)
                put("p_target_id", targetId)
                put("p_reason", reason)
                put("p_details", details)
            },
        )
    }

    override suspend fun changeBlock(
        userId: String,
        blocked: Boolean,
    ) {
        client.postgrest.rpc(
            "change_community_block",
            buildJsonObject {
                put("p_user_id", userId)
                put("p_blocked", blocked)
            },
        )
    }
}
