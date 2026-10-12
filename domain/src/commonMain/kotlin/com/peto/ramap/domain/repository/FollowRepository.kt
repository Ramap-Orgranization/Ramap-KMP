package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowCounts
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.model.community.PublicProfile

interface FollowRepository {
    suspend fun fetchCounts(): RamapResult<FollowCounts>

    suspend fun fetchConnections(
        list: FollowList,
        offset: Long,
    ): RamapResult<List<PublicProfile>>

    suspend fun changeFollow(
        userId: String,
        action: FollowAction,
    ): RamapResult<Unit>
}
