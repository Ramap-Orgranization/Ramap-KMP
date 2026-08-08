package com.peto.ramap.data.datasource.importation

import com.peto.ramap.data.model.ImportationRequest
import com.peto.ramap.network.client.importation.ImportationResponse

internal interface ImportationDataSource {
    suspend fun analyze(request: ImportationRequest): ImportationResponse
}
