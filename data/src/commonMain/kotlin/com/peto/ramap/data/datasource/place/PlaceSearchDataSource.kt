package com.peto.ramap.data.datasource.place

import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.data.model.PlaceSearchResponse

internal interface PlaceSearchDataSource {
    suspend fun search(request: PlaceSearchRequest): PlaceSearchResponse
}
