package com.peto.ramap.data.datasource.place

import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.data.model.PlaceSearchResponse

interface PlaceSearchDataSource {
    suspend fun search(request: PlaceSearchRequest): PlaceSearchResponse
}
