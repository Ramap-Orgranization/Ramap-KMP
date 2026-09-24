package com.peto.ramap.data.datasource.profile

import com.peto.ramap.data.model.ProfileResponse
import com.peto.ramap.domain.model.profile.ProfileImage
import kotlinx.coroutines.flow.Flow

internal interface ProfileDataSource {
    val sessionUserIds: Flow<String?>

    fun currentUserId(): String?

    suspend fun fetchProfile(userId: String): ProfileResponse

    suspend fun updateProfile(
        userId: String,
        nickname: String,
        avatarPath: String?,
        removePhoto: Boolean,
        bio: String? = null,
        instagramUsername: String? = null,
    ): ProfileResponse

    suspend fun uploadPhoto(
        userId: String,
        path: String,
        image: ProfileImage,
    )

    suspend fun deletePhoto(
        userId: String,
        path: String,
    )

    suspend fun signedPhotoUrl(
        userId: String,
        path: String,
    ): String
}
