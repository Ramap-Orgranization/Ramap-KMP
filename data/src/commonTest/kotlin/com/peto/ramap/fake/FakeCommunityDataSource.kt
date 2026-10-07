package com.peto.ramap.fake

import com.peto.ramap.data.datasource.community.CommunityDataSource
import com.peto.ramap.data.model.BlockedUserResponse
import com.peto.ramap.data.model.MyCommunityProfileResponse
import com.peto.ramap.data.model.ProfileAccessResponse
import com.peto.ramap.data.model.PublicSavedShopsPageResponse
import com.peto.ramap.data.model.ReviewResponse

internal class FakeCommunityDataSource(
    private val reviews: List<ReviewResponse> = emptyList(),
    private val reviewError: Throwable? = null,
) : CommunityDataSource {
    val requestedReviewPages = mutableListOf<Pair<String, Long>>()

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): com.peto.ramap.data.model.ProfileReviewsPageResponse =
        com.peto.ramap.data.model
            .ProfileReviewsPageResponse(fetchProfileAccess(userId), fetchUserReviews(userId, offset))

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): List<ReviewResponse> {
        requestedReviewPages += userId to offset
        reviewError?.let { throw it }
        return reviews
    }

    override suspend fun fetchMyCommunityProfile(): MyCommunityProfileResponse = MyCommunityProfileResponse()

    override suspend fun fetchProfileAccess(userId: String): ProfileAccessResponse = ProfileAccessResponse(ProfileAccessResponse.UNAVAILABLE)

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): PublicSavedShopsPageResponse = PublicSavedShopsPageResponse(ProfileAccessResponse(ProfileAccessResponse.UNAVAILABLE))

    override suspend fun fetchBlockedUsers(): List<BlockedUserResponse> = emptyList()

    override suspend fun report(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
    ) = Unit

    override suspend fun changeBlock(
        userId: String,
        blocked: Boolean,
    ) = Unit
}
