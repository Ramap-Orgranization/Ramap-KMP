package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsCursor
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsPage

interface ShopSuggestionsRepository {
    suspend fun fetchSuggestions(
        cursor: ShopSuggestionsCursor?,
        limit: Int,
    ): RamapResult<ShopSuggestionsPage>
}
