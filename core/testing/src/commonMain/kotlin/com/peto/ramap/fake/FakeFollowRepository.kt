package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowCounts
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.repository.FollowRepository
import kotlinx.coroutines.CompletableDeferred

class FakeFollowRepository : FollowRepository {
    var countsResult: RamapResult<FollowCounts> = RamapResult.Success(FollowCounts(0L, 0L, 0L))
    var countsPending: CompletableDeferred<RamapResult<FollowCounts>>? = null
    var changeResult: RamapResult<Unit> = RamapResult.Success(Unit)
    var connections: Map<FollowList, List<PublicProfile>> = emptyMap()
    val calls = mutableListOf<Pair<FollowList, Long>>()

    override suspend fun fetchCounts(): RamapResult<FollowCounts> = countsPending?.await() ?: countsResult

    override suspend fun fetchConnections(
        list: FollowList,
        offset: Long,
    ): RamapResult<List<PublicProfile>> {
        calls += list to offset
        return RamapResult.Success(connections[list].orEmpty().drop(offset.toInt()).take(20))
    }

    override suspend fun changeFollow(
        userId: String,
        action: FollowAction,
    ): RamapResult<Unit> {
        if (changeResult is RamapResult.Error) return changeResult
        val request = connections[FollowList.REQUESTS].orEmpty().firstOrNull { it.userId == userId }
        val list =
            when (action) {
                FollowAction.APPROVE, FollowAction.REJECT -> FollowList.REQUESTS
                FollowAction.REMOVE -> FollowList.FOLLOWERS
                FollowAction.UNFOLLOW, FollowAction.FOLLOW -> FollowList.FOLLOWING
            }
        connections = connections + (list to connections[list].orEmpty().filterNot { it.userId == userId })
        if (action == FollowAction.APPROVE && request != null) {
            connections = connections + (FollowList.FOLLOWERS to (listOf(request) + connections[FollowList.FOLLOWERS].orEmpty()))
        }
        val counts = (countsResult as? RamapResult.Success)?.data
        if (counts != null) countsResult = RamapResult.Success(counts.after(action))
        return changeResult
    }
}
