package com.peto.ramap.data.datasource.geocoder

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.model.ReverseGeocodeRequest
import com.peto.ramap.data.model.ReverseGeocodeResponse
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.repository.ReverseGeocoder
import com.peto.ramap.network.execute.invokeRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body

internal class SupabaseReverseGeocoder(
    private val client: SupabaseClient,
) : ReverseGeocoder {
    override suspend fun address(location: Location): RamapResult<String?> =
        invokeRequest {
            client.functions
                .invoke(
                    function = FUNCTION_NAME,
                    body = ReverseGeocodeRequest(location.lat, location.lng),
                ).body<ReverseGeocodeResponse>()
                .address
        }

    private companion object {
        const val FUNCTION_NAME = "reverse-geocode"
    }
}
