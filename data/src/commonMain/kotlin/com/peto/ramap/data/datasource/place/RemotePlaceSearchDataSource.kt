package com.peto.ramap.data.datasource.place

import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.data.model.PlaceSearchResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.HttpHeaders

internal class RemotePlaceSearchDataSource(
    private val client: SupabaseClient,
) : PlaceSearchDataSource {
    override suspend fun search(request: PlaceSearchRequest): PlaceSearchResponse =
        try {
            client.functions
                .invoke(
                    function = FUNCTION_NAME,
                    body = request,
                ).body()
        } catch (exception: RestException) {
            throw PlaceSearchErrorParser.parse(
                status = exception.statusCode,
                body = exception.error,
                retryAfterHeader = exception.response.headers[HttpHeaders.RetryAfter],
            ) ?: exception
        }

    private companion object {
        private const val FUNCTION_NAME = "place-search"
    }
}
