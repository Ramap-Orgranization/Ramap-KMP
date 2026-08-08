package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.importation.ImportationDataSource
import com.peto.ramap.data.model.ImportationRequest
import com.peto.ramap.domain.model.importation.ImportationPreview
import com.peto.ramap.domain.model.importation.ImportationProvider
import com.peto.ramap.domain.repository.ImportationRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultImportationRepository(
    private val dataSource: ImportationDataSource,
) : ImportationRepository {
    override suspend fun analyze(url: String): RamapResult<ImportationPreview> =
        invokeRequest {
            val response = dataSource.analyze(ImportationRequest(url))
            ImportationPreview(
                provider = ImportationProvider.valueOf(response.provider.uppercase()),
                totalPlaceCount = response.totalPlaceCount,
                matchedShopIds = response.matchedShopIds.toSet(),
                unmatchedPlaceNames = response.unmatchedPlaceNames,
            )
        }
}
