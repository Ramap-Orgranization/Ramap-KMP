package com.peto.ramap.network.client.importation

import com.peto.ramap.domain.repository.ImportationErrorCode
import com.peto.ramap.domain.repository.ImportationException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Supabase Edge Function 호출과 가져오기 전용 오류 변환을 담당한다. */
class ImportationFunctionClient(
    private val client: SupabaseClient,
) {
    /** Function을 호출하고 응답 오류 코드를 도메인 예외로 변환한다. */
    suspend fun invoke(
        functionName: String,
        body: JsonElement,
    ): ImportationResponse =
        try {
            client.functions.invoke(function = functionName, body = body).body()
        } catch (exception: RestException) {
            throw ImportationException(parseErrorCode(exception.error) ?: throw exception)
        }

    /** Function 오류 응답 본문에서 가져오기 오류 코드를 추출한다. */
    private fun parseErrorCode(body: String): ImportationErrorCode? =
        runCatching { Json.decodeFromString<ImportationErrorResponse>(body).code }.getOrNull()?.let {
            when (it) {
                UNSUPPORTED_URL_ERROR_CODE -> ImportationErrorCode.UNSUPPORTED_URL
                UNAVAILABLE_LIST_ERROR_CODE -> ImportationErrorCode.UNAVAILABLE_LIST
                PROVIDER_FAILURE_ERROR_CODE -> ImportationErrorCode.PROVIDER_FAILURE
                else -> null
            }
        }

    private companion object {
        const val UNSUPPORTED_URL_ERROR_CODE = "unsupported_url"
        const val UNAVAILABLE_LIST_ERROR_CODE = "unavailable_list"
        const val PROVIDER_FAILURE_ERROR_CODE = "provider_failure"
    }
}
