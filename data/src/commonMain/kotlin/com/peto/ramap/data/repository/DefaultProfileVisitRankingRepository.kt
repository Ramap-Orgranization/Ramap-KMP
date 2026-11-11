package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.profile.ProfileVisitRankingDataSource
import com.peto.ramap.domain.model.profile.ProfileVisitRanking
import com.peto.ramap.domain.repository.ProfileVisitRankingRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultProfileVisitRankingRepository(
    private val dataSource: ProfileVisitRankingDataSource,
) : ProfileVisitRankingRepository {
    override suspend fun fetchProfileVisitRanking(userId: String): RamapResult<ProfileVisitRanking?> = invokeRequest { dataSource.fetchProfileVisitRanking(userId).toDomain() }
}
