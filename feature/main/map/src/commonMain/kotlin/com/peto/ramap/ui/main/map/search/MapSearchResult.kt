package com.peto.ramap.ui.main.map.search

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery

sealed interface MapSearchResult {
    data object Cleared : MapSearchResult

    data class Loaded(
        val query: SearchQuery,
        val shops: RamenShops,
        val places: PlaceSearchResults,
    ) : MapSearchResult

    data class Failed(
        val error: RamapError,
    ) : MapSearchResult
}
