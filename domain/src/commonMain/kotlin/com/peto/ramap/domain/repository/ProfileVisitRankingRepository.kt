package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.ProfileVisitRanking

interface ProfileVisitRankingRepository {
    suspend fun fetchProfileVisitRanking(userId: String): RamapResult<ProfileVisitRanking?>
}
