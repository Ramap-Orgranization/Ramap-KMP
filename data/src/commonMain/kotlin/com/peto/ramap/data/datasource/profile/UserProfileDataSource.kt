package com.peto.ramap.data.datasource.profile

import com.peto.ramap.data.model.MyProfileResponse
import com.peto.ramap.data.model.ProfileAccessResponse
import com.peto.ramap.data.model.ProfileReviewsPageResponse
import com.peto.ramap.data.model.PublicSavedShopsPageResponse
import com.peto.ramap.data.model.ReviewResponse

internal interface UserProfileDataSource {
    suspend fun fetchMyProfile(): MyProfileResponse

    suspend fun fetchProfileAccess(userId: String): ProfileAccessResponse

    suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): ProfileReviewsPageResponse

    suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): List<ReviewResponse>

    suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): PublicSavedShopsPageResponse
}
