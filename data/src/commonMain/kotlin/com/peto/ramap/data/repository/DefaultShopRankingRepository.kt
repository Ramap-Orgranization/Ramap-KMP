package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.ranking.ShopRankingDataSource
import com.peto.ramap.data.datasource.ranking.toRequest
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.repository.ShopRankingRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultShopRankingRepository(
    private val dataSource: ShopRankingDataSource,
) : ShopRankingRepository {
    override suspend fun fetchShopRankings(query: RankingQuery): RamapResult<RankingPage> =
        invokeRequest { dataSource.fetchShopRankings(query.toRequest()) }
}
