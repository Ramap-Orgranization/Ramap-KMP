package com.peto.ramap.ui.main.ranking

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.repository.ShopRankingRepository

class FakeShopRankingRepository(
    var page: RankingPage = RankingPage(ShopRankings(emptyList()), null),
) : ShopRankingRepository {
    val queries = mutableListOf<RankingQuery>()
    var error: RamapError? = null

    override suspend fun fetchShopRankings(query: RankingQuery): RamapResult<RankingPage> {
        queries += query
        error?.let { return RamapResult.Error(it) }
        return RamapResult.Success(page)
    }
}
