package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.domain.model.rank.RankingPage

interface ShopRankingDataSource {
    suspend fun fetchShopRankings(parameter: ShopRankingParameters): RankingPage
}
