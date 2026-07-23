package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.domain.model.rank.RankingPage

internal interface ShopRankingDataSource {
    suspend fun fetchShopRankings(parameter: ShopRankingParameters): RankingPage
}
