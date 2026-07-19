package com.peto.ramap.data.datasource.place

import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.data.model.PlaceSearchResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body

class RemotePlaceSearchDataSource(
    private val client: SupabaseClient,
) : PlaceSearchDataSource {
    override suspend fun search(request: PlaceSearchRequest): PlaceSearchResponse =
        client.functions
            .invoke(
                function = FUNCTION_NAME,
                body = request,
            ).body()

    private companion object {
        private const val FUNCTION_NAME = "place-search"
    }
}
