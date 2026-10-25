package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.ProfileReviewsPage
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.PublicSavedShopsPage

interface UserProfileRepository {
    suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?>

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
