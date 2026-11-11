package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsCursor
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsPage
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ShopSuggestionsRepository
import kotlinx.coroutines.CompletableDeferred

public class FakeShopSuggestionsRepository(
    shops: RamenShops,
) : ShopSuggestionsRepository {
    public data class Request(
        val cursor: ShopSuggestionsCursor?,
        val limit: Int,
    )

    public var error: RamapError? = null
    public var pages: Map<ShopSuggestionsCursor?, ShopSuggestionsPage> = mapOf(null to ShopSuggestionsPage(shops, null))
    public var pendingRequest: CompletableDeferred<Unit>? = null
    public val requests: MutableList<Request> = mutableListOf()

    override suspend fun fetchSuggestions(
        cursor: ShopSuggestionsCursor?,
        limit: Int,
    ): RamapResult<ShopSuggestionsPage> {
        requests += Request(cursor, limit)
        pendingRequest?.await()
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(pages.getValue(cursor))
    }
}
