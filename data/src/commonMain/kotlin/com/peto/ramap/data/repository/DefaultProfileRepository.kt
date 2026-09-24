package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.profile.ProfileDataSource
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultProfileRepository(
    private val dataSource: ProfileDataSource,
) : ProfileRepository {
    override val sessionUserIds = dataSource.sessionUserIds

    override suspend fun fetchMyProfile(): RamapResult<AccountProfile> =
        invokeRequest {
            val userId = requireNotNull(dataSource.currentUserId()) { "Missing authenticated user" }
            val response = dataSource.fetchProfile(userId)
            check(dataSource.currentUserId() == userId && response.userId == userId) { "Profile session changed" }
            val avatarUrl = response.avatarPath?.let { dataSource.signedPhotoUrl(userId, it) }
            check(dataSource.currentUserId() == userId) { "Profile session changed" }
            AccountProfile(userId, response.nickname, avatarUrl, response.bio, response.instagramUsername)
        }
}
