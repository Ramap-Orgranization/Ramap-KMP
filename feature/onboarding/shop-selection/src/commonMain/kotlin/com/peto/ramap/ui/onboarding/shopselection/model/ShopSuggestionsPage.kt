package com.peto.ramap.ui.onboarding.shopselection.model

import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.shop.RamenShops

internal data class ShopSuggestionsPage(
    val shops: RamenShops,
    val nextCursor: RankingCursor?,
)
