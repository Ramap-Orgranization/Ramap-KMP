package com.peto.ramap.domain.model.onboarding

import com.peto.ramap.domain.model.shop.RamenShops

data class ShopSuggestionsPage(
    val shops: RamenShops,
    val nextCursor: ShopSuggestionsCursor?,
)
