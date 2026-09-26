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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

internal class DefaultProfileRepository(
    private val dataSource: ProfileDataSource,
) : ProfileRepository {
    private val operations = Mutex()
    override val sessionUserIds = dataSource.sessionUserIds

    override suspend fun fetchMyProfile(): RamapResult<AccountProfile> =
        invokeRequest {
            val userId = requireNotNull(dataSource.currentUserId()) { ERROR_MISSING_AUTHENTICATED_USER }
            operations.withLock {
                checkSession(userId)
                accountProfile(userId, dataSource.fetchProfile(userId))
            }
        }

    override suspend fun isNicknameAvailable(nickname: String): RamapResult<Boolean> =
        invokeRequest {
            require(ProfileNickname(nickname).isValid) { ERROR_INVALID_PROFILE_NICKNAME }
            val userId = requireNotNull(dataSource.currentUserId()) { ERROR_MISSING_AUTHENTICATED_USER }
            val available = dataSource.isNicknameAvailable(nickname)
            checkSession(userId)
            available
        }

    override suspend fun updateMyProfile(draft: ProfileDraft): RamapResult<AccountProfile> =
        invokeRequest {
            draft.validate()
            val userId = requireNotNull(dataSource.currentUserId()) { ERROR_MISSING_AUTHENTICATED_USER }
            operations.withLock {
                updateProfile(
                    userId = userId,
                    nickname = draft.nickname.value,
                    image = draft.image,
                    removePhoto = draft.removePhoto,
                    bio = draft.bio?.value,
                )
            }
        }

    private suspend fun updateProfile(
        userId: String,
        nickname: String,
        image: ProfileImage?,
        removePhoto: Boolean,
        bio: String?,
    ): AccountProfile {
        checkSession(userId)
        val previous = dataSource.fetchProfile(userId)
        checkSession(userId)
        check(previous.userId == userId) { ERROR_MISMATCHED_PROFILE_OWNER }
        val path = image?.let { "$userId/${Uuid.random()}.${it.fileExtension}" }
        if (path != null) uploadPhoto(userId, path, image)
        try {
            checkSession(userId)
        } catch (exception: Throwable) {
            if (path != null) cleanupPhoto(userId, path)
            throw exception
        }
        val response = dataSource.updateProfile(userId, nickname, path, removePhoto, bio)
        checkSession(userId)
        check(response.userId == userId) { ERROR_MISMATCHED_PROFILE_OWNER }
        if (previous.avatarPath != null && previous.avatarPath != response.avatarPath) {
            cleanupPhoto(userId, previous.avatarPath)
        }
        return accountProfile(userId, response)
    }

    private suspend fun uploadPhoto(
        userId: String,
        path: String,
        image: ProfileImage,
    ) {
        try {
            dataSource.uploadPhoto(userId, path, image)
        } catch (exception: Throwable) {
            cleanupPhoto(userId, path)
            throw exception
        }
    }

    private suspend fun cleanupPhoto(
        userId: String,
        path: String,
    ) = withContext(NonCancellable) {
        try {
            if (dataSource.currentUserId() == userId) dataSource.deletePhoto(userId, path)
        } catch (_: Exception) {
            // Best-effort cleanup must preserve the original failure, including cancellation.
        }
    }

    private suspend fun accountProfile(
        userId: String,
        response: ProfileResponse,
    ): AccountProfile {
        checkSession(userId)
        check(response.userId == userId) { ERROR_MISMATCHED_PROFILE_OWNER }
        val avatarUrl = response.avatarPath?.let { dataSource.signedPhotoUrl(userId, it) }
        checkSession(userId)
        return AccountProfile(
            userId = userId,
            nickname = response.nickname,
            avatarUrl = avatarUrl,
            bio = response.bio,
        )
    }

    private fun checkSession(userId: String) {
        check(dataSource.currentUserId() == userId) { ERROR_PROFILE_SESSION_CHANGED }
    }

    private companion object {
        const val ERROR_MISSING_AUTHENTICATED_USER = "Missing authenticated user"
        const val ERROR_INVALID_PROFILE_NICKNAME = "Invalid profile nickname"
        const val ERROR_MISMATCHED_PROFILE_OWNER = "Mismatched profile owner"
        const val ERROR_PROFILE_SESSION_CHANGED = "Profile session changed"
    }
}
