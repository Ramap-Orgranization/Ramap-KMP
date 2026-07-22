package com.peto.ramap.domain.model.rank

data class ShopRankings(
    private val values: List<ShopRanking>,
) : List<ShopRanking> by values {
    fun appendNextPage(nextPage: ShopRankings): ShopRankings {
        val newRankings = selectNewRankings(nextPage)
        return ShopRankings(values + newRankings)
    }

    fun assignDenseRanks(): List<RankedShop> {
        var previousLikeCount: Long? = null
        var denseRank = 0
        val rankedShops = mutableListOf<RankedShop>()

        for (ranking in values) {
            if (startsNextRank(ranking, previousLikeCount)) {
                denseRank += 1
            }
            rankedShops.add(RankedShop(rank = denseRank, ranking = ranking))
            previousLikeCount = ranking.likeCount
        }
        return rankedShops
    }

    private fun selectNewRankings(nextPage: ShopRankings): List<ShopRanking> {
        val seenShopIds = collectShopIds()
        val newRankings = mutableListOf<ShopRanking>()

        for (ranking in nextPage) {
            if (seenShopIds.add(ranking.shop.id)) {
                newRankings.add(ranking)
            }
        }
        return newRankings
    }

    private fun collectShopIds(): MutableSet<String> {
        val shopIds = mutableSetOf<String>()
        for (ranking in values) {
            shopIds.add(ranking.shop.id)
        }
        return shopIds
    }

    private fun startsNextRank(
        ranking: ShopRanking,
        previousLikeCount: Long?,
    ): Boolean = ranking.likeCount != previousLikeCount
}
