package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.domain.repository.ProfileVisitRankingRepository

class FakeProfileVisitRankingRepository(
    var fetch: suspend (String) -> RamapResult<ProfileVisitRanking?> = { RamapResult.Success(null) },
) : ProfileVisitRankingRepository {
    override suspend fun fetchProfileVisitRanking(userId: String): RamapResult<ProfileVisitRanking?> = fetch(userId)
}
