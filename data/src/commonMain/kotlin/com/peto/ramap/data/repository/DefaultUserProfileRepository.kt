package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.profile.UserProfileDataSource
import com.peto.ramap.domain.model.profile.ProfileAccess
import com.peto.ramap.domain.model.profile.ProfileReviewsPage
import com.peto.ramap.domain.model.profile.PublicProfile
import com.peto.ramap.domain.model.profile.PublicSavedShopsPage
import com.peto.ramap.domain.model.review.ProfileReview
import com.peto.ramap.domain.repository.UserProfileRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultUserProfileRepository(
    private val dataSource: UserProfileDataSource,
) : UserProfileRepository {
    override suspend fun fetchMyProfile(): RamapResult<PublicProfile?> = invokeRequest { dataSource.fetchMyProfile().toDomain() }

    override suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess> = invokeRequest { dataSource.fetchProfileAccess(userId).toDomain() }

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): RamapResult<ProfileReviewsPage> =
        invokeRequest {
            val page = dataSource.fetchProfileReviewsPage(userId, offset)
            ProfileReviewsPage(
                access = page.access.toDomain(),
                reviews = page.reviews.map { it.toDomain() },
            )
        }

    override suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<ProfileReview>> =
        invokeRequest {
            dataSource.fetchProfileReviewsPage(userId, offset).reviews.map { it.toDomain() }
        }

    override suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): RamapResult<PublicSavedShopsPage> = invokeRequest { dataSource.fetchUserSavedShops(userId, offset).toDomain() }
}
