package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.community.ReviewCommunityDataSource
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.repository.ReviewCommunityRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultReviewCommunityRepository(
    private val dataSource: ReviewCommunityDataSource,
) : ReviewCommunityRepository {
    override suspend fun fetchBlockedUsers(): RamapResult<List<PublicProfile>> =
        invokeRequest {
            dataSource.fetchBlockedUsers().map { it.toDomain() }
        }
}
