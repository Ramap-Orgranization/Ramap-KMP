package com.peto.ramap.data.datasource.profile

import com.peto.ramap.data.model.ProfileVisitRankingResponse

internal interface ProfileVisitRankingDataSource {
    suspend fun fetchProfileVisitRanking(userId: String): ProfileVisitRankingResponse
}
