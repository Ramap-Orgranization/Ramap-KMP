package com.peto.ramap.fake

import com.peto.ramap.data.datasource.place.PlaceSearchDataSource
import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.data.model.PlaceSearchResponse

class FakePlaceSearchDataSource(
    private val response: PlaceSearchResponse,
) : PlaceSearchDataSource {
    var request: PlaceSearchRequest? = null

    override suspend fun search(request: PlaceSearchRequest): PlaceSearchResponse {
        this.request = request
        return response
    }
}
