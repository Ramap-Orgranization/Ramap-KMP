package com.peto.ramap.data.datasource.community

import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.model.BlockedUserResponse
import com.peto.ramap.data.model.MyCommunityProfileResponse
import com.peto.ramap.data.model.ProfileAccessResponse
import com.peto.ramap.data.model.PublicSavedShopsPageResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.SignedImageRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteCommunityDataSource(
    private val client: SupabaseClient,
    private val reviewDataSource: ReviewDataSource,
    private val imageUrlDataSource: ImageUrlDataSource,
) : CommunityDataSource {
    override suspend fun fetchMyCommunityProfile(): MyCommunityProfileResponse {
        val profile =
            client.postgrest
                .rpc(RPC_FETCH_MY_COMMUNITY_MEMBERSHIP)
                .decodeAs<MyCommunityProfileResponse>()
        return profile.copy(avatarUrl = signedAvatar(profile.avatarPath))
    }

    override suspend fun fetchProfileAccess(userId: String): ProfileAccessResponse {
        val access =
            client.postgrest
                .rpc(
                    RPC_FETCH_COMMUNITY_PROFILE_ACCESS,
                    buildJsonObject { put(PARAM_USER_ID, userId) },
                ).decodeAs<ProfileAccessResponse>()
        if (access.status != ProfileAccessResponse.VISIBLE) return access
        val profile = access.profile ?: return access
        return access.copy(profile = profile.copy(avatarUrl = signedAvatar(profile.avatarPath)))
    }

    private suspend fun signedAvatar(path: String?): String? {
        if (path == null) return null
        return try {
            val image = SignedImageRequest(bucket = BUCKET_PROFILE_AVATARS, path = path)
            imageUrlDataSource.fetchUrls(listOf(image))[image]?.url
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

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): PublicSavedShopsPageResponse =
        client.postgrest
            .rpc(
                RPC_FETCH_PROFILE_SAVED_SHOPS,
                buildJsonObject {
                    put(PARAM_USER_ID, userId)
                    put(PARAM_OFFSET, offset)
                },
            ).decodeAs<PublicSavedShopsPageResponse>()

    override suspend fun fetchBlockedUsers(): List<BlockedUserResponse> {
        val profiles =
            client.postgrest
                .rpc(RPC_FETCH_BLOCKED_COMMUNITY_PROFILES)
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
        const val RPC_FETCH_COMMUNITY_PROFILE_ACCESS = "fetch_community_profile_access"
        const val RPC_FETCH_BLOCKED_COMMUNITY_PROFILES = "fetch_blocked_community_profiles_v2"
        const val RPC_REPORT_COMMUNITY_CONTENT = "report_community_content"
        const val RPC_CHANGE_COMMUNITY_BLOCK = "change_community_block"

        const val RPC_FETCH_PROFILE_SAVED_SHOPS = "fetch_profile_saved_shops"
        const val PARAM_OFFSET = "p_offset"
        const val PARAM_USER_ID = "p_user_id"
        const val PARAM_TARGET_TYPE = "p_target_type"
        const val PARAM_TARGET_ID = "p_target_id"
        const val PARAM_REASON = "p_reason"
        const val PARAM_DETAILS = "p_details"
        const val PARAM_BLOCKED = "p_blocked"

        const val BUCKET_PROFILE_AVATARS = "profile-avatars"
    }
}
