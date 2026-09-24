package com.peto.ramap.data.datasource.profile

import com.peto.ramap.data.model.ProfileResponse
import kotlinx.coroutines.flow.Flow

internal interface ProfileDataSource {
    val sessionUserIds: Flow<String?>

    fun currentUserId(): String?

    suspend fun fetchProfile(userId: String): ProfileResponse

    suspend fun signedPhotoUrl(
        userId: String,
        path: String,
    ): String
}
