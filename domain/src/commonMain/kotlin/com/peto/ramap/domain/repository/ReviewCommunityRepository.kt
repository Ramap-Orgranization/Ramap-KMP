package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.PublicProfile

interface ReviewCommunityRepository {
    suspend fun fetchBlockedUsers(): RamapResult<List<PublicProfile>>
}
