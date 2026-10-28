package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.ProfileAccess
import com.peto.ramap.domain.model.profile.ProfileReviewsPage
import com.peto.ramap.domain.model.profile.PublicProfile
import com.peto.ramap.domain.model.profile.PublicSavedShopsPage
import com.peto.ramap.domain.model.review.ProfileReview

interface UserProfileRepository {
    suspend fun fetchMyProfile(): RamapResult<PublicProfile?>

    suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess>

    suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): RamapResult<ProfileReviewsPage>

    suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<ProfileReview>>

    suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): RamapResult<PublicSavedShopsPage>
}
