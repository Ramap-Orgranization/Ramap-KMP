package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.onboarding.ShopSuggestionsDataSource
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsCursor
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsPage
import com.peto.ramap.domain.repository.ShopSuggestionsRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultShopSuggestionsRepository(
    private val dataSource: ShopSuggestionsDataSource,
) : ShopSuggestionsRepository {
    override suspend fun fetchSuggestions(
        cursor: ShopSuggestionsCursor?,
        limit: Int,
    ): RamapResult<ShopSuggestionsPage> = invokeRequest { dataSource.fetchSuggestions(cursor, limit) }
}
