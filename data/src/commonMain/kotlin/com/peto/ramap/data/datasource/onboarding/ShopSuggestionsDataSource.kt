package com.peto.ramap.data.datasource.onboarding

import com.peto.ramap.domain.model.onboarding.ShopSuggestionsCursor
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsPage

internal interface ShopSuggestionsDataSource {
    suspend fun fetchSuggestions(
        cursor: ShopSuggestionsCursor?,
        limit: Int,
    ): ShopSuggestionsPage
}
