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
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes

internal class RemoteProfileDataSource(
    private val client: SupabaseClient,
) : ProfileDataSource {
    override val sessionUserIds =
        client.auth.sessionStatus
            // 백그라운드 전환 중의 세션 복원 상태는 로그아웃이 아니다.
            .filterNot { it is SessionStatus.Initializing }
            .map { status ->
                (status as? SessionStatus.Authenticated)?.session?.user?.id
            }.distinctUntilChanged()

    override fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    override suspend fun fetchProfile(userId: String): ProfileResponse =
        client.postgrest
            .rpc(
                FUNCTION_FETCH_OR_CREATE_MY_PROFILE,
                buildJsonObject { put(PARAMETER_USER_ID, userId) },
            ).decodeAs()

    override suspend fun isNicknameAvailable(nickname: String): Boolean =
        client.postgrest
            .rpc(
                FUNCTION_IS_PROFILE_NICKNAME_AVAILABLE,
                buildJsonObject { put(PARAMETER_NICKNAME, nickname) },
            ).decodeAs()

    override suspend fun updateProfile(
        userId: String,
        nickname: String,
        avatarPath: String?,
        removePhoto: Boolean,
        bio: String?,
    ): ProfileResponse =
        client.postgrest
            .rpc(
                FUNCTION_UPDATE_MY_PROFILE,
                buildJsonObject {
                    put(PARAMETER_USER_ID, userId)
                    put(PARAMETER_NICKNAME, nickname)
                    put(PARAMETER_AVATAR_PATH, avatarPath)
                    put(PARAMETER_REMOVE_PHOTO, removePhoto)
                    if (bio != null) put(PARAMETER_BIO, bio)
                },
            ).decodeAs()

    override suspend fun updateProfileVisibility(isPublic: Boolean): ProfileResponse =
        client.postgrest
            .rpc(
                FUNCTION_UPDATE_MY_PROFILE_VISIBILITY,
                buildJsonObject { put(PARAMETER_IS_PUBLIC, isPublic) },
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
        check(currentUserId() == userId && path.startsWith("$userId/")) { ERROR_SESSION_CHANGED }
    }

    private companion object {
        const val BUCKET = "profile-avatars"
        val URL_LIFETIME = 30.minutes

        const val FUNCTION_FETCH_OR_CREATE_MY_PROFILE = "fetch_or_create_my_profile"
        const val FUNCTION_IS_PROFILE_NICKNAME_AVAILABLE = "is_profile_nickname_available"
        const val FUNCTION_UPDATE_MY_PROFILE = "update_my_profile"
        const val FUNCTION_UPDATE_MY_PROFILE_VISIBILITY = "update_my_profile_visibility"

        const val PARAMETER_USER_ID = "p_user_id"
        const val PARAMETER_NICKNAME = "p_nickname"
        const val PARAMETER_AVATAR_PATH = "p_avatar_path"
        const val PARAMETER_REMOVE_PHOTO = "p_remove_photo"
        const val PARAMETER_BIO = "p_bio"
        const val PARAMETER_IS_PUBLIC = "p_is_public"

        const val ERROR_SESSION_CHANGED = "Profile session changed"
    }
}
