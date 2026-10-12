package com.peto.ramap.data.datasource.follow

import com.peto.ramap.data.model.FollowCountsResponse
import com.peto.ramap.data.model.PublicProfileResponse

internal interface FollowDataSource {
    suspend fun fetchCounts(): FollowCountsResponse

    suspend fun fetchConnections(
        list: String,
        offset: Long,
    ): List<PublicProfileResponse>

    suspend fun changeFollow(
        userId: String,
        action: String,
    )
}
