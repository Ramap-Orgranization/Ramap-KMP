package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.follow.FollowAction
import com.peto.ramap.domain.model.follow.FollowCounts
import com.peto.ramap.domain.model.follow.FollowList
import com.peto.ramap.domain.model.profile.PublicProfile

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
