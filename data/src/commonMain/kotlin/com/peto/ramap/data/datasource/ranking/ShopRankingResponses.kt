package com.peto.ramap.data.datasource.ranking

import com.peto.ramap.data.model.ShopRankingResponse
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.ShopRankings

internal fun List<ShopRankingResponse>.toDomain(limit: Int): RankingPage {
    val hasNext = size > limit
    val pageItems = take(limit)
    val nextCursor = pageItems.lastOrNull()?.toCursor()?.takeIf { hasNext }
    return RankingPage(
        items = ShopRankings(pageItems.map(ShopRankingResponse::toDomain)),
        nextCursor = nextCursor,
    )
}
