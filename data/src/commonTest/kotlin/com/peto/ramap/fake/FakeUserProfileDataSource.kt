package com.peto.ramap.fake

import com.peto.ramap.data.datasource.profile.UserProfileDataSource
import com.peto.ramap.data.model.MyProfileResponse
import com.peto.ramap.data.model.ProfileAccessResponse
import com.peto.ramap.data.model.ProfileReviewResponse
import com.peto.ramap.data.model.ProfileReviewsPageResponse
import com.peto.ramap.data.model.PublicSavedShopsPageResponse
import com.peto.ramap.data.model.ReviewResponse

internal class FakeUserProfileDataSource(
    private val reviews: List<ProfileReviewResponse> = emptyList(),
    private val reviewError: Throwable? = null,
) : UserProfileDataSource {
    val requestedReviewPages = mutableListOf<Pair<String, Long>>()

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): ProfileReviewsPageResponse {
        requestedReviewPages += userId to offset
        reviewError?.let { throw it }
        return ProfileReviewsPageResponse(fetchProfileAccess(userId), reviews)
    }

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): List<ReviewResponse> {
        requestedReviewPages += userId to offset
        reviewError?.let { throw it }
        return reviews.map { it.review }
    }

    override suspend fun fetchMyProfile(): MyProfileResponse = MyProfileResponse()

    override suspend fun fetchProfileAccess(userId: String): ProfileAccessResponse = ProfileAccessResponse(ProfileAccessResponse.UNAVAILABLE)

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): PublicSavedShopsPageResponse = PublicSavedShopsPageResponse(ProfileAccessResponse(ProfileAccessResponse.UNAVAILABLE))
}
