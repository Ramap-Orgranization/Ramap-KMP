package com.peto.ramap.data.datasource.importation

import com.peto.ramap.data.model.ImportationRequest
import com.peto.ramap.network.client.importation.ImportationFunctionClient
import com.peto.ramap.network.client.importation.ImportationResponse
import kotlinx.serialization.json.Json

internal class KakaoImportationDataSource(
    private val functionClient: ImportationFunctionClient,
) {
    suspend fun analyze(url: String): ImportationResponse =
        functionClient.invoke(
            FUNCTION_NAME,
            Json.encodeToJsonElement(ImportationRequest.serializer(), ImportationRequest(url)),
        )

    private companion object {
        const val FUNCTION_NAME = "kakao-importation"
    }
}
