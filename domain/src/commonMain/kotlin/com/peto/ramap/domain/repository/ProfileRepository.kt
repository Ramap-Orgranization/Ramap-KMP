package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val sessionUserIds: Flow<String?>

    suspend fun fetchMyProfile(): RamapResult<AccountProfile>

    suspend fun updateMyProfile(
        nickname: String,
        image: ProfileImage? = null,
        removePhoto: Boolean = false,
        bio: String? = null,
        instagramUsername: String? = null,
    ): RamapResult<AccountProfile>
}
