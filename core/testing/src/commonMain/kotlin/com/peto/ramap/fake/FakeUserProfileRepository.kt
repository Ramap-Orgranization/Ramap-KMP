package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.PublicSavedShopsPage
import com.peto.ramap.domain.repository.UserProfileRepository

class FakeUserProfileRepository : UserProfileRepository {
    override suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?> = RamapResult.Success(PublicProfile("me", "나"))

    override suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess> = RamapResult.Success(ProfileAccess.Unavailable)

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): RamapResult<com.peto.ramap.domain.model.community.ProfileReviewsPage> =
        RamapResult.Success(
            com.peto.ramap.domain.model.community
                .ProfileReviewsPage(ProfileAccess.Unavailable, emptyList()),
        )

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<ProfileReview>> = RamapResult.Success(emptyList())

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): RamapResult<PublicSavedShopsPage> = RamapResult.Success(PublicSavedShopsPage(ProfileAccess.Unavailable, emptyList()))
}
