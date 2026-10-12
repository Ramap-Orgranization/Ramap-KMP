package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.follow.FollowDataSource
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowCounts
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.repository.FollowRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultFollowRepository(
    private val dataSource: FollowDataSource,
) : FollowRepository {
    override suspend fun fetchCounts(): RamapResult<FollowCounts> = invokeRequest { dataSource.fetchCounts().toDomain() }

    override suspend fun fetchConnections(
        list: FollowList,
        offset: Long,
    ): RamapResult<List<PublicProfile>> =
        invokeRequest {
            dataSource.fetchConnections(list.name.lowercase(), offset).map { it.toDomain() }
        }

    override suspend fun changeFollow(
        userId: String,
        action: FollowAction,
    ): RamapResult<Unit> =
        invokeRequest {
            dataSource.changeFollow(userId, action.name.lowercase())
        }
}
