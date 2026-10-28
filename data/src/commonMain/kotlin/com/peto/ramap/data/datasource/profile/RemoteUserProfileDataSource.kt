package com.peto.ramap.data.datasource.profile

import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.model.MyProfileResponse
import com.peto.ramap.data.model.ProfileAccessResponse
import com.peto.ramap.data.model.ProfileReviewsPageResponse
import com.peto.ramap.data.model.PublicSavedShopsPageResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.SignedImageRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteUserProfileDataSource(
    private val client: SupabaseClient,
    private val reviewDataSource: ReviewDataSource,
    private val imageUrlDataSource: ImageUrlDataSource,
) : UserProfileDataSource {
    override suspend fun fetchMyProfile(): MyProfileResponse {
        val profile =
            client.postgrest
                .rpc(RPC_FETCH_MY_PROFILE)
                .decodeAs<MyProfileResponse>()
        return profile.copy(avatarUrl = signedAvatar(profile.avatarPath))
    }

    override suspend fun fetchProfileAccess(userId: String): ProfileAccessResponse {
        val access =
            client.postgrest
                .rpc(
                    RPC_FETCH_PROFILE_ACCESS,
                    buildJsonObject { put(PARAM_USER_ID, userId) },
                ).decodeAs<ProfileAccessResponse>()
        return signProfileAccess(access)
    }

    private suspend fun signProfileAccess(access: ProfileAccessResponse): ProfileAccessResponse {
        if (access.status != ProfileAccessResponse.VISIBLE) return access
        val profile = access.profile ?: return access
        return access.copy(profile = profile.copy(avatarUrl = signedAvatar(profile.avatarPath)))
    }

    private suspend fun signedAvatar(path: String?): String? {
        if (path == null) return null
        return try {
            val image = SignedImageRequest(BUCKET_PROFILE_AVATARS, path)
            imageUrlDataSource.fetchUrls(listOf(image))[image]?.url
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): ProfileReviewsPageResponse {
        val page = reviewDataSource.fetchProfileReviewsPage(userId, offset)
        return page.copy(access = signProfileAccess(page.access))
    }

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): List<ReviewResponse> = reviewDataSource.fetchProfileReviews(userId, offset)

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): PublicSavedShopsPageResponse {
        val page =
            client.postgrest
                .rpc(
                    RPC_FETCH_PROFILE_SAVED_SHOPS,
                    buildJsonObject {
                        put(PARAM_USER_ID, userId)
                        put(PARAM_OFFSET, offset)
                    },
                ).decodeAs<PublicSavedShopsPageResponse>()
        return page.copy(access = signProfileAccess(page.access))
    }

    private companion object {
        const val RPC_FETCH_MY_PROFILE = "fetch_my_community_membership"
        const val RPC_FETCH_PROFILE_ACCESS = "fetch_community_profile_access"
        const val RPC_FETCH_PROFILE_SAVED_SHOPS = "fetch_profile_saved_shops"
        const val PARAM_OFFSET = "p_offset"
        const val PARAM_USER_ID = "p_user_id"
        const val BUCKET_PROFILE_AVATARS = "profile-avatars"
    }
}
