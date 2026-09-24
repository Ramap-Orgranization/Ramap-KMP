package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.AccountProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val sessionUserIds: Flow<String?>

    suspend fun fetchMyProfile(): RamapResult<AccountProfile>
}
