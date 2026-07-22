package com.peto.ramap.domain.model.rank

data class RankingPage(
    val items: ShopRankings,
    val nextCursor: RankingCursor?,
) {
    val hasNext: Boolean
        get() = nextCursor != null
}
