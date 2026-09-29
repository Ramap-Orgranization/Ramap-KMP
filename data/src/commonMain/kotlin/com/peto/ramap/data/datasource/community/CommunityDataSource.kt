package com.peto.ramap.data.datasource.community

import com.peto.ramap.data.model.MyCommunityProfileResponse
import com.peto.ramap.data.model.PublicProfileResponse
import com.peto.ramap.data.model.ReviewResponse

internal interface CommunityDataSource {
    suspend fun fetchMyCommunityProfile(): MyCommunityProfileResponse

    suspend fun fetchPublicProfile(userId: String): PublicProfileResponse?

    suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): List<ReviewResponse>

    suspend fun fetchBlockedUsers(): List<PublicProfileResponse>

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
