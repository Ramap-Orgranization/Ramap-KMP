package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.community.CommunityDataSource
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.ProfileReviewsPage
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.PublicSavedShopsPage
import com.peto.ramap.domain.repository.UserProfileRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultUserProfileRepository(
    private val dataSource: CommunityDataSource,
) : UserProfileRepository {
    override suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?> = invokeRequest { dataSource.fetchMyCommunityProfile().toDomain() }

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
