package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.place.PlaceSearchDataSource
import com.peto.ramap.data.model.PlaceSearchCenterRequest
import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.domain.model.place.PlaceSearchResults
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.PlaceSearchRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultPlaceSearchRepository(
    private val dataSource: PlaceSearchDataSource,
) : PlaceSearchRepository {
    override suspend fun search(
        query: SearchQuery,
        center: Location,
    ): RamapResult<PlaceSearchResults> =
        invokeRequest {
            PlaceSearchResults(
                dataSource
                    .search(
                        PlaceSearchRequest(
                            query = query.value,
                            center = PlaceSearchCenterRequest(center.lat, center.lng),
                        ),
                    ).results
                    .map { it.toDomain() },
            )
        }
}
