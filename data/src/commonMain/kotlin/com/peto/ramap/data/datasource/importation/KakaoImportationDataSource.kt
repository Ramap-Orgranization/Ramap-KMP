package com.peto.ramap.data.datasource.importation

import com.peto.ramap.data.model.ImportationRequest
import com.peto.ramap.network.client.importation.ImportationFunctionClient
import com.peto.ramap.network.client.importation.ImportationResponse
import kotlinx.serialization.json.Json

/** 카카오 지도 가져오기 Function을 호출한다. */
internal class KakaoImportationDataSource(
    private val functionClient: ImportationFunctionClient,
) {
    /** 카카오 공유 URL을 Function에 전달해 장소 매칭 결과를 반환한다. */
    suspend fun analyze(url: String): ImportationResponse =
        functionClient.invoke(
            FUNCTION_NAME,
            Json.encodeToJsonElement(ImportationRequest.serializer(), ImportationRequest(url)),
        )

    private companion object {
        const val FUNCTION_NAME = "kakao-importation"
    }
}
