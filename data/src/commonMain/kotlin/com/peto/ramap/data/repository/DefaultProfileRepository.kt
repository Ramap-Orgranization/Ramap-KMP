package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.profile.ProfileDataSource
import com.peto.ramap.data.model.ProfileResponse
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileDraft
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.domain.model.profile.ProfileNickname
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.network.execute.invokeRequest
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class DefaultProfileRepository(
    private val dataSource: ProfileDataSource,
    private val currentTime: () -> Instant = { Clock.System.now() },
) : ProfileRepository {
    override val sessionUserIds = dataSource.sessionUserIds

    private var avatarCache: AvatarCache? = null

    override suspend fun fetchMyProfile(): RamapResult<AccountProfile> =
        invokeRequest {
            val userId = requireNotNull(dataSource.currentUserId()) { ERROR_MISSING_AUTHENTICATED_USER }
            accountProfile(userId, dataSource.fetchProfile(userId))
        }

    override suspend fun isNicknameAvailable(nickname: String): RamapResult<Boolean> =
        invokeRequest {
            require(ProfileNickname(nickname).isValid) { ERROR_INVALID_PROFILE_NICKNAME }
            dataSource.isNicknameAvailable(nickname)
        }

    override suspend fun updateMyProfile(draft: ProfileDraft): RamapResult<AccountProfile> =
        invokeRequest {
            draft.validate()
            val userId = requireNotNull(dataSource.currentUserId()) { ERROR_MISSING_AUTHENTICATED_USER }
            updateProfile(
                userId = userId,
                nickname = draft.nickname.value,
                image = draft.image,
                removePhoto = draft.removePhoto,
                bio = draft.bio?.value,
            )
        }

    override suspend fun updateProfileVisibility(isPublic: Boolean): RamapResult<AccountProfile> =
        invokeRequest {
            val userId = requireNotNull(dataSource.currentUserId()) { ERROR_MISSING_AUTHENTICATED_USER }
            val response = dataSource.updateProfileVisibility(isPublic)
            accountProfile(userId, response, tolerateAvatarSigningFailure = true)
        }

    private suspend fun updateProfile(
        userId: String,
        nickname: String,
        image: ProfileImage?,
        removePhoto: Boolean,
        bio: String?,
    ): AccountProfile {
        val previousAvatarPath = previousAvatarPath(userId, image, removePhoto)
        val path = image?.let { "$userId/${Uuid.random()}.${it.fileExtension}" }
        if (path != null) {
            dataSource.uploadPhoto(userId, path, image)
        }
        val response =
            try {
                dataSource.updateProfile(userId, nickname, path, removePhoto, bio)
            } catch (error: Throwable) {
                rethrowProfileUpdateFailure(userId, path, (error as? RestException)?.statusCode, error)
            }
        check(response.userId == userId) { ERROR_MISMATCHED_PROFILE_OWNER }
        cleanUpPreviousAvatar(userId, previousAvatarPath, response.avatarPath)
        return accountProfile(userId, response, tolerateAvatarSigningFailure = true)
    }

    private suspend fun previousAvatarPath(
        userId: String,
        image: ProfileImage?,
        removePhoto: Boolean,
    ): String? {
        if (image == null && !removePhoto) return null
        val previous = dataSource.fetchProfile(userId)
        check(previous.userId == userId) { ERROR_MISMATCHED_PROFILE_OWNER }
        return previous.avatarPath
    }

    private suspend fun cleanUpPreviousAvatar(
        userId: String,
        previousPath: String?,
        updatedPath: String?,
    ) {
        if (previousPath == null || previousPath == updatedPath) return
        withContext(NonCancellable) {
            try {
                dataSource.deletePhoto(userId, previousPath)
            } catch (_: Throwable) {
                // A completed profile save remains successful when old-photo cleanup fails.
            }
        }
        currentCoroutineContext().ensureActive()
    }

    internal suspend fun rethrowProfileUpdateFailure(
        userId: String,
        path: String?,
        status: Int?,
        error: Throwable,
    ): Nothing {
        if (path != null && status in CONFIRMED_REJECTION_STATUSES) {
            try {
                withContext(NonCancellable) { dataSource.deletePhoto(userId, path) }
            } catch (cleanupError: CancellationException) {
                throw cleanupError
            } catch (_: Throwable) {
                // Keep the original profile-save failure when best-effort cleanup also fails.
            }
        }
        throw error
    }

    private suspend fun accountProfile(
        userId: String,
        response: ProfileResponse,
        tolerateAvatarSigningFailure: Boolean = false,
    ): AccountProfile {
        check(response.userId == userId) { ERROR_MISMATCHED_PROFILE_OWNER }
        val avatarUrl =
            if (response.avatarPath != null) {
                resolveAvatarUrl(
                    userId = userId,
                    path = response.avatarPath,
                    tolerateFailure = tolerateAvatarSigningFailure,
                )
            } else {
                clearAvatarCache()
                null
            }
        return AccountProfile(
            userId = userId,
            nickname = response.nickname,
            avatarUrl = avatarUrl,
            bio = response.bio,
            isPublic = response.isPublic,
            nicknameChangesRemaining = response.nicknameChangesRemaining,
            bioChangesRemaining = response.bioChangesRemaining,
        )
    }

    private suspend fun resolveAvatarUrl(
        userId: String,
        path: String,
        tolerateFailure: Boolean,
    ): String? {
        val currentCache = avatarCache
        if (currentCache != null && currentCache.matches(userId, path, currentTime())) {
            return currentCache.url
        }
        val url =
            try {
                dataSource.signedPhotoUrl(userId, path)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                if (!tolerateFailure) throw exception
                return null
            }
        avatarCache = AvatarCache(userId = userId, path = path, url = url, signedAt = currentTime())
        return url
    }

    private fun clearAvatarCache() {
        avatarCache = null
    }

    private companion object {
        val CONFIRMED_REJECTION_STATUSES = setOf(400, 401, 403, 404, 409, 422, 429)
        const val ERROR_MISSING_AUTHENTICATED_USER = "Missing authenticated user"
        const val ERROR_INVALID_PROFILE_NICKNAME = "Invalid profile nickname"
        const val ERROR_MISMATCHED_PROFILE_OWNER = "Mismatched profile owner"
    }
}
