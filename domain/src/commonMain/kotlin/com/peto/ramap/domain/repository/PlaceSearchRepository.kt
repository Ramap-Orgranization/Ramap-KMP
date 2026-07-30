package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery

interface PlaceSearchRepository {
    suspend fun search(
        query: SearchQuery,
        center: Location,
    ): RamapResult<PlaceSearchResults>
}
