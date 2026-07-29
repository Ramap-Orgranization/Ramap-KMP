package com.peto.ramap.domain.model.rank

data class RankedShops(
    private val rankings: ShopRankings,
) : List<RankedShop> by rankings.assignDenseRanks() {
    fun appendNextPage(nextPage: ShopRankings): RankedShops {
        val appendedRankings = rankings.appendNextPage(nextPage)
        return RankedShops(appendedRankings)
    }
}
