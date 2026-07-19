package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.delay

class FakePlaceSearchRepository(
    var results: PlaceSearchResults = PlaceSearchResults(emptyList()),
    var error: RamapError? = null,
    var delayMillis: Long = 0,
) : PlaceSearchRepository {
    val requests = mutableListOf<Pair<SearchQuery, Location>>()

    override suspend fun search(
        query: SearchQuery,
        center: Location,
    ): RamapResult<PlaceSearchResults> {
        requests += query to center
        delay(delayMillis)
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(results)
    }
}
