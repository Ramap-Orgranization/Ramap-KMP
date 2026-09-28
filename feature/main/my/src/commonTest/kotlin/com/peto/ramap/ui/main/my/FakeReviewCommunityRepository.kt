package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.repository.ReviewCommunityRepository

class FakeReviewCommunityRepository : ReviewCommunityRepository {
    var blockedUsersResult: RamapResult<List<PublicProfile>> = RamapResult.Success(emptyList())

    override suspend fun fetchBlockedUsers(): RamapResult<List<PublicProfile>> = blockedUsersResult
}
