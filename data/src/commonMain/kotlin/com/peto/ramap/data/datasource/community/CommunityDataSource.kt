package com.peto.ramap.data.datasource.community

import com.peto.ramap.data.model.BlockedUserResponse
import com.peto.ramap.data.model.MyCommunityProfileResponse
import com.peto.ramap.data.model.ProfileAccessResponse
import com.peto.ramap.data.model.ProfileReviewsPageResponse
import com.peto.ramap.data.model.PublicSavedShopsPageResponse
import com.peto.ramap.data.model.ReviewResponse

internal interface CommunityDataSource {
    suspend fun fetchMyCommunityProfile(): MyCommunityProfileResponse

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

    suspend fun fetchBlockedUsers(): List<BlockedUserResponse>

    suspend fun report(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
    )

    suspend fun changeBlock(
        userId: String,
        blocked: Boolean,
    )
}
