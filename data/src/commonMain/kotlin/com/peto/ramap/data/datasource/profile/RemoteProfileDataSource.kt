package com.peto.ramap.data.datasource.profile

import com.peto.ramap.data.model.ProfileResponse
import com.peto.ramap.domain.model.profile.ProfileImage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes

internal class RemoteProfileDataSource(
    private val client: SupabaseClient,
) : ProfileDataSource {
    override val sessionUserIds =
        client.auth.sessionStatus
            .map { status ->
                (status as? SessionStatus.Authenticated)?.session?.user?.id
            }.distinctUntilChanged()

    override fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    override suspend fun fetchProfile(userId: String): ProfileResponse = client.postgrest.rpc("fetch_or_create_my_profile", buildJsonObject { put("p_user_id", userId) }).decodeAs()

    override suspend fun isNicknameAvailable(nickname: String): Boolean = client.postgrest.rpc("is_profile_nickname_available", buildJsonObject { put("p_nickname", nickname) }).decodeAs()

    override suspend fun updateProfile(
        userId: String,
        nickname: String,
        avatarPath: String?,
        removePhoto: Boolean,
        bio: String?,
    ): ProfileResponse =
        client.postgrest
            .rpc(
                "update_my_profile",
                buildJsonObject {
                    put("p_user_id", userId)
                    put("p_nickname", nickname)
                    put("p_avatar_path", avatarPath)
                    put("p_remove_photo", removePhoto)
                    if (bio != null) put("p_bio", bio)
                },
            ).decodeAs()

    override suspend fun uploadPhoto(
        userId: String,
        path: String,
        image: ProfileImage,
    ) {
        checkOwner(userId, path)
        client.storage.from(BUCKET).upload(path, image.bytes) {
            contentType = ContentType.parse(image.mimeType)
        }
    }

    override suspend fun deletePhoto(
        userId: String,
        path: String,
    ) {
        checkOwner(userId, path)
        client.storage.from(BUCKET).delete(path)
    }

    override suspend fun signedPhotoUrl(
        userId: String,
        path: String,
    ): String {
        checkOwner(userId, path)
        return client.storage.from(BUCKET).createSignedUrl(path, URL_LIFETIME)
    }

    private fun checkOwner(
        userId: String,
        path: String,
    ) {
        check(currentUserId() == userId && path.startsWith("$userId/")) { "Profile session changed" }
    }

    private companion object {
        const val BUCKET = "profile-avatars"
        val URL_LIFETIME = 30.minutes
    }
}
