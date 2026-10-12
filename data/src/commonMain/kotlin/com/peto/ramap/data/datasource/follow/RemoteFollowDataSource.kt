package com.peto.ramap.data.datasource.follow

import com.peto.ramap.data.model.FollowCountsResponse
import com.peto.ramap.data.model.PublicProfileResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes

internal class RemoteFollowDataSource(
    private val client: SupabaseClient,
) : FollowDataSource {
    override suspend fun fetchCounts(): FollowCountsResponse = client.postgrest.rpc("fetch_my_follow_counts").decodeAs<FollowCountsResponse>()

    override suspend fun fetchConnections(
        list: String,
        offset: Long,
    ): List<PublicProfileResponse> =
        coroutineScope {
            client.postgrest
                .rpc(
                    "fetch_my_follow_connections",
                    buildJsonObject {
                        put("p_list", list)
                        put("p_offset", offset)
                    },
                ).decodeList<PublicProfileResponse>()
                .map { profile -> async { profile.copy(avatarUrl = signedAvatar(profile.avatarPath)) } }
                .awaitAll()
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

    override suspend fun changeFollow(
        userId: String,
        action: String,
    ) {
        client.postgrest.rpc(
            "change_profile_follow",
            buildJsonObject {
                put("p_user_id", userId)
                put("p_action", action)
            },
        )
    }

    private companion object {
        const val BUCKET_PROFILE_AVATARS = "profile-avatars"
        val SIGNED_URL_LIFETIME = 5.minutes
    }
}
